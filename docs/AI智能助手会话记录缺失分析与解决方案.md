# AI 智能助手「左侧会话记录」缺失分析与解决方案

> 状态：`分析 + 方案设计（本次仅输出方案文档，不落地代码）`
> 目标：让 AI 智能助手左侧边栏正常展示历史会话（列表 / 新建 / 切换 / 加载消息 / 删除），并与右侧对话流打通。

---

## 一、问题现象

在「AI 智能助手」页面（`views/ai/assistant/index.vue`）：

- 右侧对话区**有内容、能正常流式回复**（说明 SSE / 模型 / 落库链路已通）。
- 左侧边栏「＋ 新建会话」下方始终显示 **「暂无历史会话」**，即使已经多次对话。

即：**后端已在落库会话，前端却没展示**。

---

## 二、现状盘点

### 2.1 后端：能力已齐备（无需改动，可复用）

会话持久化到 `ai_chat_session` / `ai_chat_message` 两张表，逻辑在 `AgentChatService`：

| 能力 | 实现位置 | 状态 |
|---|---|---|
| 流式对话建会话/落消息 | `stream()` → `ensureSessionRow()` + `saveMessage(user/assistant)` | ✅ 已实现 |
| 会话状态更新 | `stream()` FINISHED/ERROR → `updateSessionStatus()` | ✅ 已实现 |
| 会话分页 | `sessionPage(AiSessionQuery)` | ✅ 已实现 |
| 会话消息列表 | `sessionMessages(sessionId)` | ✅ 已实现 |
| 新建会话 | `newSession(agentName, message)` | ✅ 已实现 |
| 删除会话 | `deleteSession(sessionId)` | ✅ 已实现 |

对外接口（`AgentChatController`，前缀 `/admin/ai/agent`）：

```
GET    /list                        智能体列表
POST   /session/new                 新建会话   → 返回 sessionId
GET    /session/page                会话分页   → PageResult<AiSessionVO>
GET    /session/{sessionId}/messages 会话消息  → List<AiChatMessageVO>
DELETE /session/{sessionId}         删除会话
```

查询入参 `AiSessionQuery`（继承 `BaseQuery`，含 `pageNum`/`pageSize`）：

```java
private String agentName;   // 智能体名（可过滤当前智能体）
private String userType;    // admin / member
private Long   userId;      // 当前登录用户
private Integer status;     // 1-进行中 2-已完成 4-错误
private Long   tenantId;    // 租户隔离
```

登录上下文：`AiSessionContextResolver` 从 Sa-Token 解析 `userId / userType / tenantId`（管理端 `userType=admin`）。

> 结论：**后端不缺少任何功能**，左侧会话记录缺失完全在前端。

### 2.2 前端：接口已封装，页面未接入

前端 API 封装**已经就绪**：

| 文件 | 封装 | 说明 |
|---|---|---|
| `api/ai/session.ts` | `getSessionPage` | `GET /session/page` |
| `api/ai/session.ts` | `getSessionMessages` | `GET /session/{id}/messages` |
| `api/ai/session.ts` | `removeSession` | `DELETE /session/{id}` |
| `api/ai/agent.ts` | `agentNewSession` | `POST /session/new` |
| `api/ai/types.ts` | `AiSessionVO` / `AiChatMessageVO` | 类型已定义 |

但页面 `views/ai/assistant/index.vue` **完全没有调用以上接口**，具体缺失点：

1. **左侧会话列表未渲染**：`<aside>` 里是静态文案 `暂无历史会话`（`index.vue` 第 96 行），没有 `v-for` 渲染会话项。
2. **未请求会话列表**：`onMounted` 只调 `loadAgents()`，未调 `getSessionPage()`。
3. **新建会话未调后端**：`newSession()`（第 80-83 行）只清空本地 `messages`，未调用 `agentNewSession`，也未更新会话 id。
4. **无切换/加载消息逻辑**：没有「点击会话 → `getSessionMessages` → 渲染右侧」的联动。
5. **无删除会话入口**。

---

## 三、根因分析

**「左侧会话记录没有」的根本原因是：前端页面未接入会话历史 API，也未实现会话生命周期管理。**

- 右侧对话内容之所以能显示，是因为它来自前端内存中的 `messages`（`useChatStream` 的本地响应式数组），**不依赖后端**。
- 而后端在每次流式对话时已把会话写入 `ai_chat_session`（`ensureSessionRow`）、把消息写入 `ai_chat_message`（`saveMessage`）。
- 前端左侧**既不查询也不展示**这些已落库的数据，所以永远显示占位文案「暂无历史会话」。

一句话：**数据在后端有、接口在前端也已封装，唯独页面层没有把「查询 → 渲染 → 交互」串起来。**

---

## 四、处理方案（前端为主，后端可选加固）

### 4.1 总体思路

在 `views/ai/assistant/index.vue`（或抽一个 `useSessionList` hook）补齐四件事：

1. **拉取**：进入页面调 `getSessionPage` 获取当前用户、当前智能体的会话列表。
2. **渲染**：左侧 `<aside>` 用 `v-for` 渲染会话项（标题 + 最后活跃时间 + 删除按钮），空态显示「暂无历史会话」。
3. **会话生命周期**：新建 → 调 `agentNewSession` 预建并拿到 `sessionId`；切换 → 调 `getSessionMessages` 回填右侧；删除 → 确认后 `removeSession`。
4. **与流式打通**：把「当前会话 id」同步进 `useChatStream`，保证后续对话续在同一会话。

### 4.2 关键改造点与伪代码

#### ① `use-chat-stream.ts`：支持外部注入当前会话 id，并回传新会话 id

当前 `useChatStream` 自己维护 `currentSessionId`（仅依赖 SSE 的 `SESSION` 事件回填）。建议：

```ts
interface UseChatStreamOptions {
  agentName: MaybeRefOrGetter<string>;
  // 新增：由页面传入当前选中会话 id（初始为空串表示新建）
  sessionId?: MaybeRefOrGetter<string>;
  // 新增：流式结束后回调，把最终 sessionId 回传给页面
  onSessionChange?: (sessionId: string) => void;
}
```

在 `send()` 中：

```ts
await fetchStream('/admin/ai/agent/stream', {
  agentName: toValue(agentName),
  sessionId: toValue(sessionId) || undefined,   // 未选中则交给后端新建
  ...
});
```

`applyEvent` 的 `session` 分支更新 `currentSessionId` 后，通过 `onSessionChange` 同步到页面：

```ts
if (eventType === 'session') {
  if (data.sessionId) {
    currentSessionId.value = data.sessionId;
    onSessionChange?.(data.sessionId);
  }
  return;
}
```

> 说明：后端 `stream()` 中 `param.getSessionId()` 为空时会调用 `agentTemplate.newSession()` 生成新 id，并通过 `STARTED`（归一化为 `session`）事件把 `sessionId` 回传，前端据此记录，无需额外请求。

#### ② `index.vue`：会话列表状态与数据加载

新建会话列表状态：

```ts
// 会话子列表 & 消息（AiChatMessageVO[] -> ChatMessage[]）
const sessions = ref<AiSessionVO[]>([]);
const activeSessionId = ref('');

async function loadSessions() {
  const res = await getSessionPage({
    agentName: toValue(agentNameComputed),   // 当前智能体
    userType: 'admin',
    pageNum: 1,
    pageSize: 20,
  });
  sessions.value = res.list ?? [];
  // 若列表存在且未选中任何会话，默认选中最近一条
}
```

`onMounted` 同时加载智能体与会话：

```ts
onMounted(() => {
  void loadAgents();
  void loadSessions();
});
```

#### ③ 左侧渲染与会话交互

```vue
<aside class="...">
  <Button block type="primary" @click="onNewSession">＋ 新建会话</Button>

  <!-- 会话列表 -->
  <div v-if="sessions.length" class="mt-3 space-y-1">
    <div
      v-for="s in sessions"
      :key="s.sessionId"
      :class="s.sessionId === activeSessionId ? '...active...' : '...'"
      @click="onSwitchSession(s.sessionId)"
    >
      <span class="truncate">{{ s.title }}</span>
      <Button type="text" size="small" @click.stop="onRemoveSession(s.sessionId)">删除</Button>
    </div>
  </div>
  <div v-else class="mt-3 text-center text-xs text-foreground/40">暂无历史会话</div>
</aside>
```

```ts
// 新建会话：预建并切到新会话
async function onNewSession() {
  const sid = await agentNewSession({ agentName: toValue(agentNameComputed) });
  messages.value.length = 0;
  activeSessionId.value = sid;
  await loadSessions();
}

// 切换会话：回填右侧消息
async function onSwitchSession(sessionId: string) {
  activeSessionId.value = sessionId;
  messages.value.length = 0;
  const vos = await getSessionMessages(sessionId);
  messages.value = vosToMessages(vos);
}

// 删除会话
async function onRemoveSession(sessionId: string) {
  // 二次确认
  await removeSession(sessionId);
  if (activeSessionId.value === sessionId) {
    activeSessionId.value = '';
    messages.value.length = 0;
  }
  await loadSessions();
}
```

#### ④ `AiChatMessageVO → ChatMessage` 映射

```ts
function vosToMessages(vos: AiChatMessageVO[]): ChatMessage[] {
  return vos.map((vo) => ({
    id: `${vo.sessionId}-${vo.seq}`,
    role: vo.role as 'user' | 'assistant',
    content: vo.content ?? '',
    // 可选：thinking / tools 由后续模型扩展字段补齐
    status: 'done',
    tokenCost: vo.tokenUsage,
  }));
}
```

#### ⑤ 搜索/智能体切换时刷新会话

`onAgentChange` 切换智能体后，应同时 `loadSessions()`（按新 `agentName` 过滤）。

### 4.3 后端可选加固（非必须）

1. **会话分页鉴权**：`GET /session/page` 目前无 `@SaCheckPermission`，建议按项目惯例补充权限注解（如 `ai:chat:session`），与前端菜单权限对应。
2. **新建会话标题默认值**：`newSession()` 首条消息可能为空，`buildTitle` 会得到 `"新会话"`，可优化为用 `agentName` 作为默认标题。
3. **会话分页默认排序**：已按 `last_active_time` 倒序，无需改动。

---

## 五、风险与注意事项

| 项 | 说明 |
|---|---|
| 会话范围隔离 | `getSessionPage` 需携带 `userType: 'admin'` 与当前用户 `userId`，避免展示他人会话；`userId` 可从 `useUserStore` 获取，或依赖后端上下文注入时忽略传参。 |
| 流式进行中的会话 | 若会话尚未收到 `FINISHED`，其状态为进行中；切换时 `messages` 接口返回已落库的 user/assistant 消息，正在生成的片段可能缺失，属预期。 |
| 新建会话 id 来源 | 建议首次发送时让 `stream` 自动建会话并回传 `sessionId`，避免在「新建」按钮里预建导致空会话行。 |
| 消息映射兼容 | 若后端后续暴露 `thinking/toolCalls/stepCount`，映射函数同步扩展即可，不影响主流程。 |
| 菜单/路由 | 左侧会话记录功能不涉及新增菜单，仅在现有页面内实现，无需权限菜单变更。 |

---

## 六、验收标准

1. 进入「AI 智能助手」页，左侧展示**当前用户、当前智能体**的历史会话列表（标题 + 时间 + 删除），无记录时显示「暂无历史会话」。
2. 多次对话后，左侧会话列表出现新会话并可点击切换，右侧回填完整消息历史。
3. 「＋ 新建会话」能发起新会话并清空右侧，后续对话写入新会话。
4. 删除会话有二次确认，成功后列表刷新、若删的是当前会话则右侧清空。
5. 切换智能体后，会话列表按新智能体刷新。
6. 流式对话正常（回归），`vue-tsc` 与 `oxlint` 通过。

---

## 七、结论

- **根因**：AI 智能助手左侧会话记录的缺失由**前端页面未接入会话 API、未实现会话生命周期管理**导致；后端会话落库与接口均已就绪。
- **处理方案**：以 `views/ai/assistant/index.vue` 为主，复用已封装的 `session.ts / agent.ts` API，补齐「列表查询、渲染、新建、切换、加载消息、删除」闭环，并把当前会话 id 同步进 `useChatStream` 以打通流式续聊。
- **落地方式**：本次仅出方案，代码后续按本方案实施；后端仅需按需补鉴权/标题默认值等加固。
