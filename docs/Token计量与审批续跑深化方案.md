# AI 智能体 Token 计量与审批续跑深化方案

> 范围：仅针对「Token 真实计量」与「敏感工具审批后续跑」两项做深化设计。
> 定位：**方案文档，暂不改动代码**。所有结论均基于当前代码勘查与 AgentScope 2.0.2 实际 API（javap 反编译核验）。
> 关联：《AgentScope2.0整合方案.md》《AI 模型与敏感词动态化设计方案 v2》。
>
> **v2 修订（可行性验证）**：基于字节码引用检索与项目 POM/代码实测，对本方案的关键假设做了可行性验证，新增 2.3、4.7 两节，并据此**重排了第六章分期顺序**。四处新发现：
> ① `agentscope-extensions-redis` **已是 starter 既有依赖但从未接线**（路线 B 零新增依赖）；
> ② `ReActAgent.CONFIRM_SINK_KEY` 为官方确认回调接缝（业务无需轮询 Redis 捞状态）；
> ③ **事件发布实测为同步分发**，计量落库会阻塞 SSE 尾包（须先做异步化前置项）；
> ④ `buildHarness` **从未调用 `disableSessionPersistence()`**，与类注释「关闭内部会话持久化」矛盾——路线 B 开工前必须先核实该现状。

---

## 一、背景与目标

当前智能体框架（`zhush-spring-boot-starter-agentscope` + `zhush-module-ai`）已跑通「多模型 + 工具调用 + SSE 流式 + 敏感词分级 + 人工审批 + 审计落库」主链路，但存在两处「有结构、无数据 / 有挂起、无续跑」的空心能力：

1. **Token 计量空心**：`agent_audit_log.token_usage`、`agent_token_usage`（用量汇总表）、`ai_chat_message.token_usage`、`ai_chat_session.token_cost` 均有表结构与消费代码，但运行时恒为 0，成本上限与用量监控无法落地。
2. **审批无法续跑**：敏感工具命中审批后以 `ToolResultBlock.suspended()` 挂起，但① `agent_approval` 表**从未被写入**（管理端审批列表永远为空、`approve()` 必抛「会话不存在」）；② 审批通过后被挂起的工具调用**不会重放**，用户需重新发起，审批闭环断裂。

目标：补齐上述两项，使其从「可用占位」达到「生产闭环」。

---

## 二、AgentScope 2.0.2 能力勘察结论（决策依据）

以下为 `javap` 反编译 `agentscope-core / harness / extensions-redis 2.0.2` 得到的**真实 API**，直接决定方案可行性：

### 2.1 Token 计量相关

| API | 说明 |
|---|---|
| `io.agentscope.core.model.ChatUsage` | `getInputTokens()/getOutputTokens()/getCachedTokens()/getTotalTokens():int`、`getTime():double`、`builder()` |
| `Msg.getUsage()` / `Msg.getChatUsage()` | 同步调用返回的最终 `Msg` 直接携带本次 usage |
| `AgentEventType.MODEL_CALL_END` → `ModelCallEndEvent` | `getUsage():ChatUsage`、`getReplyId()` —— **每一次模型调用（即每一步推理）结束时产出**，是流式计量的采集点 |
| `AgentEventType.AGENT_RESULT` → `AgentResultEvent` | `getResult():Msg` —— 最终结果消息（含 usage），可作为兜底采集点 |

> 结论：**真实 token 无需自建分词估算**，AgentScope 逐次模型调用已回传 `ChatUsage`，直接累加即可。`MODEL_CALL_END` 次数即天然「推理步数 stepCount」。

### 2.2 审批续跑相关

| API | 说明 |
|---|---|
| `AgentEventType.REQUIRE_USER_CONFIRM` → `RequireUserConfirmEvent` | `getToolCalls():List<ToolUseBlock>`、`getReplyId()` —— agent 请求确认并暂停 |
| `AgentEventType.USER_CONFIRM_RESULT` → `UserConfirmResultEvent` | `getConfirmResults():List<ConfirmResult>`、`getReplyId()` |
| `io.agentscope.core.event.ConfirmResult` | `new ConfirmResult(boolean confirmed, ToolUseBlock, List<PermissionRule>)`、`isConfirmed()`、`getToolCall()` |
| `ToolUseBlock` | `getId()/getName()/getInput():Map/withState(ToolCallState)` —— 携带完整待执行调用，可改参 |
| `Msg.METADATA_CONFIRM_RESULTS` / `METADATA_CONFIRM_REQUEST_REPLY_ID` | 以「确认结果消息」喂回 agent 恢复的元数据键 |
| `HarnessAgent.Builder.enablePendingToolRecovery(boolean)` | **原生「挂起工具恢复」开关** |
| `HarnessAgent.Builder.stateStore(AgentStateStore)` / `distributedStore(...)` / `defaultSessionId(String)` / `stopOnReject(boolean)` / `permissionContext(...)` | 状态持久化与权限门禁配置入口 |
| `HarnessAgent.getStateStore()/getAgentState()/clearStateCache()` | 运行态可访问 |
| `AgentStateStore`（接口） | `save(session,agent,key,State)`、`get(...,Class):Optional`、`exists(session,agent)`、`listSessionIds` |
| `ReActAgent.CONFIRM_SINK_KEY` | 常量值 `"io.agentscope.core.ReActAgent.confirmSink"`；配合 `RuntimeContext.Builder.put(String,Object)` **注入确认回调**——官方业务接缝（v2 新增） |
| `ReActAgent` 状态 API | `saveAgentState(RuntimeContext)` / `getAgentState(sessionId,agentId)` / `clearStateCache(...)` / `replacePermissionContext(session,agent,ctx)` / `getStateStore()` / `getDelegate()` —— 支撑跨实例与重启后恢复（v2 新增） |
| `PermissionMode` | `DEFAULT / ACCEPT_EDITS / EXPLORE / BYPASS / DONT_ASK`（规则驱动确认，可运行时替换上下文） |
| `agentscope-extensions-redis` | 提供 `RedisAgentStateStore`、`RedissonAgentStateStore`、`JedisAgentStateStore`、`RedisDistributedStore`；`RedissonAgentStateStore.builder().redissonClient(client).keyPrefix(prefix).build()` 即可构造 |

> 结论：AgentScope 原生即具备「确认暂停 → 状态持久化 → 结果喂回 → 恢复续跑」的完整能力。当前项目**未启用**（`buildHarness` 未设 `stateStore`、未开 `enablePendingToolRecovery`，且自管历史 `toMsgs` 仅重建 user/assistant 文本），导致只能拿到 `suspended` 占位而无续跑。

### 2.3 字节码级可行性验证（v2 新增）

上述 API 的存在不等于运行时会发出。对 `agentscope-core-2.0.2.jar` 解包后按类文件常量池检索，得到**决定性证据**：

| 验证项 | 检索证据 | 结论 |
|---|---|---|
| 框架是否真发 usage 事件 | `ReActAgent$CallExecution` 同时引用 `ModelCallEndEvent`、`RequireUserConfirmEvent`、`ChatUsage` | ✅ ReAct 主循环内发出，非死代码 |
| usage 是否真被填充 | `ChatUsage` 被 `Msg$Builder`、`AssistantMessage$Builder`、`agent\accumulator\ReasoningContext`（累加器）引用 | ✅ 沿消息对象累加，`Msg.getUsage()` 可取 |
| 确认暂停/恢复是否闭环 | `ReActAgent$CallExecution` **既发** `RequireUserConfirmEvent`、**又消费** `UserConfirmResultEvent` 与 `Msg.METADATA_CONFIRM_RESULTS` | ✅ 暂停与恢复在同一类内实现 |
| 挂起语义归属 | `ToolSuspendException` 被 `tool\ToolExecutor`、`ReflectiveFunctionTool`、`ToolMethodInvoker` 引用（**工具层**） | ⚠️ 与权限层 confirm（**执行前**）是两个不同位置，见 4.7 |
| 采集点是否已被现有代码丢弃 | `DefaultAgentTemplate.mapEvent` 仅处理 4 类事件，其余 `return null` + `mapNotNull` | ⚠️ `MODEL_CALL_END` **当前被静默丢弃**，加分支即可，零侵入 |
| 事件分发是否阻塞 | `AgentEventPublisher.publish` 为同步 for 循环，且在 `stream()` 的 `doOnNext` 内调用 | ❌ **同步分发**，监听器任何 DB 写都会阻塞 SSE 尾包 |
| Redis 状态存储是否需新增依赖 | `zhush-spring-boot-starter-agentscope/pom.xml` L46-49 已声明 `agentscope-extensions-redis`（注释「Redis 状态后端」），但 `AgentScopeAutoConfiguration` **从未引用** | ✅ 已备料未接线，**零新增依赖** |
| Redisson 客户端是否可用 | `zhush-dependencies` 含 `redisson-spring-boot-starter:4.6.1`；`RedisCache` 已注入 `ObjectProvider<RedissonClient>` | ✅ 可直接构造 `RedissonAgentStateStore` |

**判定**：Token 计量**证据链完整、风险低**；路线 B **技术前提 100% 具备**（依赖、存储、API 接缝全在），其真实成本不在「接 Redis」，而在**会话契约重构**（详见 4.7）。

---

## 三、第一部分：Token 真实计量

### 3.1 现状问题定位

- `DefaultAgentTemplate.finishEvent(...)`：`inputTokens(0)`、`outputTokens(0)`、`stepCount(1)` **硬编码**（`DefaultAgentTemplate.java` L347-362）。
- `AgentAuditEventListener`（L44-48）已消费 `finishedEvent.getOutputTokens()+getInputTokens()` 写 `agent_audit_log.token_usage` —— 因上游恒 0 而恒 0。
- `agent_token_usage` 表：仅 `AgentTokenUsageEntity` + Mapper，**无任何写入/聚合代码**。
- `ai_chat_message.token_usage`（JSON）、`ai_chat_session.token_cost`：`AgentChatService.saveMessage`（L202-219）**未回填**。
- `ai_model` 表**无单价字段**，`agent_token_usage.cost` 无定价来源。

### 3.2 采集与聚合设计

**调用级累加器**：`stream()` 每次进入创建局部累加（`int[] in/out/cached, stepCount`，或轻量 record）：

```
MODEL_CALL_END 事件 → acc.in += usage.inputTokens
                      acc.out += usage.outputTokens
                      acc.cached += usage.cachedTokens
                      acc.steps++               // 一次模型调用 = 一步
AGENT_END / 完成       → 用 acc 构造 AgentFinishedEvent（inputTokens/outputTokens/stepCount）
```

- 同步 `chat()` / `callOnce()`：直接 `Msg reply = agent.call(...)` → `reply.getUsage()` 取本次 usage（单次调用场景，`stepCount` 可记 1 或由 hook 补，见 3.5）。
- `AgentFinishedEvent` 现有字段（`inputTokens/outputTokens/stepCount/durationMillis`）已足够，**无需扩结构**；建议**新增 `model`（模型编码）字段**以便成本定价（见 3.4），或在监听侧按 `agentName` 反查 `AgentDefinition.getModel()`。

**两个必须同时改的出口（v2 实测补充）**：`stream()` 存在**两条**构造 `finishEvent` 的路径——`mapEvent` 内的 `AGENT_END` 分支，与 `concatWith(Mono.defer(...))` 内的兜底分支（当 `finished[0]==false` 时）。累加器需以**闭包可变状态**（同现有 `StringBuilder acc` / `boolean[] finished` 风格）共享给两处，**漏改任一条就会恒为 0**。`AGENT_RESULT`（`AgentResultEvent.getResult():Msg`，含 usage）当前也未处理，可作为**权威兜底采集点**。

> 注意多步 ReAct：一次对话可能多次 `MODEL_CALL_END`（推理→工具→再推理），必须**累加**而非覆盖；`cachedTokens` 单独留存不参与「输入+输出」合计（`total = input+output`，缓存通常已含在 input 内，按供应商语义以 `getTotalTokens()` 为权威）。

> **计费口径提醒（v2 新增）**：当前 `chat()/stream()` 每轮都通过 `toMsgs(sessionManager.loadHistory(sid))` **重发全量历史**，因此 `inputTokens` 会随会话变长而增长、跨轮累加会重复计入共享上下文。这与供应商真实计费一致（每次调用确实收费），但与用户直觉的「这条消息用了多少 token」不同，展示层需明确区分「本次调用量」与「会话累计量」。

### 3.3 事件透传到落库

链路：`AgentTemplate` → `AgentEventPublisher` → 监听器扇出。分两个监听器职责：

1. **`AgentAuditEventListener`（改造）**：`FINISHED` 分支已读 token，改为读取真实 `getInput/OutputTokens`；补 `stepCount` 真实值。（`agent_audit_log` 记录单次事件粒度，已就绪）
2. **新增 `AgentUsageEventListener`（建议）**：仅处理 `FINISHED`，驱动**日聚合 upsert** 与**会话累计**：
   - `agent_token_usage` 按 `(usage_date, tenant_id, agent_name)` upsert：`request_count+1`、`input_tokens+=`、`output_tokens+=`、`total_tokens+=`、`cost+=`。
   - 依赖 `DynamicConfigRefreshCoordinator` 无关，是独立事件消费。

### 3.4 成本估算（cost）

`agent_token_usage.cost DECIMAL(12,4)` 需单价来源，二选一：

- **方案①（推荐，动态化一致）**：`ai_model` 增两列
  `input_price DECIMAL(12,6)`、`output_price DECIMAL(12,6)`（单位：元/千 token），
  或复用 `options` JSON 放 `pricing:{input,output}`。`AiModelService` 维护、`ModelConfig` 透传，事件侧按 `model` 查价：
  `cost = (in/1000)*inputPrice + (out/1000)*outputPrice`，`BigDecimal` 计算，缺价记 0 并 WARN。
- **方案②（轻量）**：字典维护「模型→单价」（前端字典集中管理规范同源），后端启动加载为内存表。

> 一期建议（对应分期 **P2**）：单价缺省 0（只计量 token 不算钱），表结构与链路先通，定价随 **P5** 补充，避免阻塞。

### 3.5 会话/消息粒度回填

- `ai_chat_message.token_usage`（JSON `{input,output,total}`）：在 `AgentChatService.stream()` 的 `FINISHED` 回调 `saveMessage(assistant)` 时，把本次 usage 一并写入（需在落 assistant 消息时携带 usage —— 由 `AgentFinishedEvent` 提供）。
- `ai_chat_session.token_cost`：`saveMessage`/新增 `accumulateSessionCost(sessionId, tokens)` 累加。
- `stepCount`：ReAct 多步下由 `MODEL_CALL_END` 计数；若走同步 `chat()` 无法逐步计数，可退化为 1（标注近似）。

### 3.6 并发与性能

- 日聚合 upsert 高频：用 **`INSERT ... ON DUPLICATE KEY UPDATE`**（MyBatis-Flex 自定义方法）保证原子；或 **Redis 计数器**（`agentscope:usage:{date}:{tenant}:{agent}`）累加 + 定时（复用协调器调度线程）刷库，降低写放大。**建议先直连 upsert**，量级上来再引入 Redis 缓冲。
- **事件发布已确认为同步分发（v2 修正）**：`AgentEventPublisher.publish` 为同步 for 循环，且调用点在 `stream()` 的 `doOnNext` / `finishEvent` 链路上——**监听器内任何 DB 写都会直接阻塞 SSE 尾包**（现 `AgentAuditEventListener` 已存在此隐性阻塞）。因此：**异步化不是优化项而是前置项**（先于计量接入，列入分期 P1），且异常必须吞掉不阻断主流程。
- **唯一键含逻辑删除位（v2 新增）**：`agent_token_usage` 的 `uk_date_tenant_agent(usage_date, tenant_id, agent_name, is_delete)` 包含 `is_delete`，聚合行**不得软删**（多条软删行会撞唯一键）。

### 3.6b 待实测项（v2 新增）

1. 供应商**流式模式**下是否确实回传 usage（部分兼容 OpenAI 服务需 `stream_options.include_usage`）——若不回传，需降级为 `AGENT_RESULT`/同步估算。
2. `CompactionConfig` / `maxContextTokens` 压缩是否影响 input token 口径。
3. `LocalModel/Ollama` 不回填 usage 时的 0 值处理（必须 `Optional` 语义，不得 NPE）。

### 3.7 会员端配额联动（对齐既有决策）

记忆「AI 会员月度配额扣减与用量采集」：`FINISHED` 后若 `userType=member`，按本次 `total` 扣减月度配额并校验上限。本方案保证 usage 真实后，配额逻辑才有输入。**P2 先只做采集**，配额闸门随 **P5** 接入。

### 3.8 改造清单（Token 计量）

| # | 文件 | 改动 |
|---|---|---|
| 0 | `AgentEventPublisher`（**v2 新增前置项**） | 改为异步分发（注入线程池）或提供异步包装，避免计量/审计落库阻塞 SSE |
| 1 | `DefaultAgentTemplate` | 流式新增 `MODEL_CALL_END` 分支累加 usage；**`AGENT_END` 与 `concatWith` 两个出口均用累加值**；同步分支读 `reply.getUsage()`；`AgentFinishedEvent` 携带 model |
| 2 | `AgentFinishedEvent` | 可选新增 `String model` / `int cachedTokens` 字段 |
| 3 | `AgentAuditEventListener` | 读真实 token/step（代码已就绪，仅上游补值即生效） |
| 4 | 新增 `AgentUsageEventListener` | `FINISHED` → `agent_token_usage` 日聚合 upsert |
| 5 | 新增 `AgentTokenUsageMapper` upsert 方法 + `AiUsageProperties`（开关） | 聚合 SQL（注意聚合行不软删） |
| 6 | `AgentChatService` | assistant 消息写 `token_usage`；会话累加 `token_cost` |
| 7 | `ai_model` DDL / `AiModelEntity`/`ModelConfig`/`AiModelService` | 单价字段（cost 用，可后置） |

---

## 四、第二部分：敏感工具审批续跑

### 4.1 现状与致命缺陷

1. **`agent_approval` 无写入链路**（实测）：全模块仅 `approve()/reject()` 对该表 `update`，**无 `insert`**。审批请求只进 Redis（`DefaultToolApprovalHandler.persist`），事件 `APPROVAL_REQUIRED` 只落 `agent_audit_log`。→ 管理端 `AgentApprovalService.page()` 恒空、`approve()` 因 `selectByApprovalId==null` 抛 `CONVERSATION_NOT_FOUND`。**审批功能实际不可用**。
2. **挂起不续跑**：`DefaultAgentTemplate.approvalGate`（L390-445）命中审批 → `ToolApprovalHandler.submit()` 返回 PENDING → `return ToolResultBlock.suspended(...)`。`agent` 该轮直接结束，**审批通过后无人重放**，Redis 里 `APPROVED` 状态成为孤儿。
3. **自管历史冲突**：`toMsgs`（L273-287）只重建 user/assistant 文本，丢弃 tool/system，与「从中断点恢复」所需的完整消息配对矛盾。

### 4.2 两条落地路线

#### 路线 A（推荐先行 · 改造小 · 不依赖 AgentScope 内部状态）

**「审批即代理执行 + 结果回注」**：把审批通过理解为「授权后端以最终入参执行该工具，并把结果作为新一轮上下文喂回模型续跑」。

流程：
1. `approvalGate` 命中 → 生成 `approvalId`，**写 `agent_approval`（补 insert：approvalId/sessionId/agentName/toolName/toolArgs=原始入参JSON/proposer/expireTime/status=0）** + 写 Redis（保留）+ 发 `APPROVAL_REQUIRED` 事件。返回 `suspended`（当前行为）。
2. 管理端 `approve(approvalId, modifiedArgs)` → 更新 Redis + `agent_approval.status=1` + `modified_args`。
3. **新增续跑服务 `ApprovalResumeService.resume(approvalId)`**：
   - 读取 `agent_approval`（toolName、finalArgs = modifiedArgs||toolArgs、sessionId、agentName）；
   - 直接经 `ToolkitRegistry` 反射执行该工具一次（`RegisteredTool` → 等价 `ReflectiveAgentTool.invoke`），得 `toolResult`；
   - 将「assistant 调用了 tool + tool 返回结果」以约定文本追加进 `AgentSessionManager` 历史（补齐 role=assistant/tool 或退化为 assistant 综合），**再次调用 `agentTemplate.stream/chat`** 让模型基于工具结果生成最终答复；
   - 结果通过 **SSE 会话通道** 推给前端（见 4.5）。
4. `reject()` → `status=2` + 记 `reject_reason`，不续跑；可选回注「操作被驳回」文本让模型给用户解释。

> 优点：契合现有「自管历史 + Redis 审批」架构，改造集中在 module-ai 业务层；无需引入 AgentScope 状态存储。缺点：续跑时 LLM 上下文靠回注重建，复杂多工具链保真度有限。

#### 路线 B（终态 · 接入 AgentScope 原生确认机制）

启用原生 `REQUIRE_USER_CONFIRM` + 状态持久化，实现真正「暂停-恢复」：

1. `AgentScopeAutoConfiguration`：为 `HarnessAgent.Builder` 增 `.stateStore(RedissonAgentStateStore.builder().redissonClient(client).keyPrefix(...).build())` + `.enablePendingToolRecovery(true)` + 统一 `defaultSessionId`/`RuntimeContext.sessionId` 作为 state session key。
   > **v2 修正（重读代码后）**：原描述「移除 `disableSessionPersistence`（若已隐式生效）」不准确——实测 `buildHarness` **从未调用** `disableSessionPersistence()`，而类注释却声称「关闭 AgentScope 内部会话持久化」。两者矛盾，说明**现状很可能已在往本地磁盘写 state**（`HarnessAgent` 有 `defaultStateDir(...)`、`isLocalSession(AgentStateStore)` 专门判断本地/远程存储差异），仅因无人读取而未暴露。**B 开工第一步必须先运行期核实此项**，否则状态来源不明会污染后续所有判断。
2. 敏感工具不再由业务 `gate` 返回 `suspended`，改为经 `permissionContext`/`PermissionMode` 让 **AgentScope 自行发 `REQUIRE_USER_CONFIRM`**（携带 `List<ToolUseBlock>` + `replyId`）并暂停，agent state 落 Redis。
3. 业务监听 `REQUIRE_USER_CONFIRM` → 转成 `AgentApprovalRequiredEvent`（携带 `replyId`、`toolCalls`、`sessionId`）→ 落 `agent_approval`（`approvalId` 关联 `replyId`）。
4. 审批 `approve` → 构造 `ConfirmResult(true, toolUseBlock(可改参 input), rules)`，以「确认结果消息」（`Msg.METADATA_CONFIRM_RESULTS` + `METADATA_CONFIRM_REQUEST_REPLY_ID=replyId`）在**同一 session key** 重新 `streamEvents`，AgentScope 从 state 恢复，跳过已确认工具、执行并发 `USER_CONFIRM_RESULT`，继续 ReAct 直到 `AGENT_END`。`reject` → `ConfirmResult(false,...)`（配合 `stopOnReject`）。
5. **（v2 补充）优先用官方接缝而非自捞**：通过 `RuntimeContext.Builder.put(ReActAgent.CONFIRM_SINK_KEY, sink)` 注入确认回调，避免轮询 Redis 捞状态；审批通过后可用 `replacePermissionContext(sessionId, agentId, ctx)` 把 `ConfirmResult` 携带的 `List<PermissionRule>` 沉淀为规则，使同类调用**免重复审批**（A 路线做不到）。多实例/重启场景需显式 `saveAgentState(rc)` 落盘。

> 优点：多步/多工具上下文天然保真，官方语义。缺点：改造深入 starter、需处理 state 序列化与 `toMsgs` 自管历史的双轨收敛（建议 B 落地后废弃 `toMsgs` 文本重建，统一交给 AgentScope state）。

**决策建议（v2 修正）**：**A 先落地（P0 + P3）修复可用性并补 `agent_approval` 写入链路**（1-2 天量级），**B 作为后续演进（P6）**，但**必须先过 P4 会话事实源收敛这道门**（涉及 starter 重构与状态双轨治理，可行性详见 4.7）。二者共享 4.3/4.4 的数据与状态设计。

### 4.3 数据与状态关联模型

统一三元关联，A/B 通用：

```
sessionId (ai_chat_session/Redis会话)
  └─ approvalId (agent_approval / Redis agentscope:approval:{id})
       ├─ B 路线额外：replyId (AgentScope 本轮 require-confirm 标识) —— 存 agent_approval 新增列 reply_id
       └─ 续跑幂等键：approvalId（一次审批仅续跑一次）
```

- `agent_approval` 建议增列：`reply_id`（B 用）、`resumed` tinyint（0/1，是否已续跑，幂等）、`resume_time`。
- `ai_chat_session.status` 已有 `3-审批挂起`：命中审批时置 3，续跑完成置 2/回 1。

### 4.4 生命周期与异常处理

| 场景 | 处理 |
|---|---|
| 重复审批 | `agent_approval.status` 非 0 直接拒绝（Redis 已有 PENDING 校验，DB 对齐） |
| 审批过期 | Redis TTL 到期 → 定时/懒更新 `status=3 已过期`；过期不可续跑 |
| 续跑幂等 | `resumed=1` 后不可再次续跑；`resume()` 入口 CAS 0→1 |
| 越权审批 | `approve/reject` 走 `@SaCheckPermission agent:approval:approve/reject`（菜单已建，接口需补注解，见 4.6） |
| 工具执行失败 | 续跑执行抛错 → 记 `reject_reason`/审计 `ERROR`，回注失败说明让模型兜底答复 |
| 无 Redis 降级 | `NullToolApprovalHandler` 现状直接放行；续跑依赖 Redis 状态，降级路径需明确（建议降级即执行不审批，保持放行） |

### 4.5 前端续跑结果回推

审批是「异步、跨请求」的，续跑产出的答复需推回用户会话：

- **P3（路线 A）**：`resume()` 产出的续跑流写库（`ai_chat_message`），前端会话切换/轮询 `GET /session/{id}/messages` 拉取；或在原对话页对 `status=3` 会话开一条**独立 SSE**（`GET /admin/ai/agent/session/{id}/resume-stream`）接收续跑增量。
- **P6（路线 B）**：原生 `USER_CONFIRM_RESULT`→后续 `TEXT_DELTA` 直接经该 SSE 通道流式推送，体验与首次对话一致（**通道本身在 P3 就需建好，B 仅复用，不新增能力**，v2 修正）。
- 前端 `agent/approval` 页已具备 approve/reject 交互，补「改参」表单（`modifiedArgs`）与「已挂起」标记。

### 4.6 改造清单（审批续跑）

| # | 文件 | 改动 |
|---|---|---|
| 1 | `DefaultAgentTemplate.approvalGate` | 命中审批时**写 `agent_approval`（经新 SPI/回调）**；保留 suspended（A）或改走原生 confirm（B） |
| 2 | 新增 `ApprovalPersister` SPI（starter 定义、module-ai 实现） | 解耦：starter 不直连业务库，回调落 `agent_approval` |
| 3 | `AgentApprovalService.approve` | DB+Redis 双更新后**触发 `ApprovalResumeService.resume()`**（A）；补 `@Transactional` 边界 |
| 4 | 新增 `ApprovalResumeService`（A 核心） | 代理执行工具 + 回注历史 + 二次 `stream/chat` + 幂等 `resumed` |
| 5 | `AgentApprovalController` | approve/reject 补 `@SaCheckPermission`；新增改参入参（`AgentApprovalParam.modifiedArgs` 已有） |
| 6 | `agent_approval` DDL | 增 `reply_id`/`resumed`/`resume_time`（B/幂等） |
| 7 | starter：`buildHarness` + `AgentScopeAutoConfiguration`（B） | 注入 `RedissonAgentStateStore`（**依赖已在 pom，零新增**）、`enablePendingToolRecovery(true)`、session key 统一；**收敛 `toMsgs` 双轨（B 的真正工作量）** |
| 8 | SSE 续跑通道（4.5） | 新增 resume-stream 端点或消息轮询（**A/B 共用，B 不可省略**） |
| 9 | `DefaultAgentTemplate.approvalGate`（B） | 门禁位置从**工具层 `suspended`** 迁移至**权限层 `REQUIRE_USER_CONFIRM`**，二者必须互斥，否则双门禁死锁（v2 新增） |
| 10 | 运行期核实（B 前置） | 确认 `disableSessionPersistence` 现状语义与是否已在写本地 state 目录（v2 新增） |

### 4.7 路线 B 可行性再评估（v2 新增）

**结论：技术前提 100% 具备，但改造量从「业务层」上移到「starter 会话契约层」，判定为中高改造量，仍列后置阶段。**

有利证据（可行性上限）：

| 项 | 实测结果 |
|---|---|
| 依赖成本 | `agentscope-extensions-redis` **已在 starter pom**，仅未接线；Redisson 4.6.1 全站可用 → **零新增依赖** |
| 原生闭环 | `ReActAgent$CallExecution` 既发 `RequireUserConfirmEvent` 又消费 `UserConfirmResultEvent` → 暂停/恢复已在框架内完成 |
| 业务接缝 | `CONFIRM_SINK_KEY` + `RuntimeContext.Builder.put(...)` → 无需轮询 Redis 捞审批状态 |
| 状态恢复 | `saveAgentState/getAgentState/clearStateCache` + `RedissonAgentStateStore` → 多实例、重启后可恢复 |
| 体验增益 | `ConfirmResult` 携带 `List<PermissionRule>`，可沉淀为免重复审批规则（**A 做不到**） |

关键障碍（改造量下限，B 不能当作「接个 Redis 就好」）：

1. **历史双轨冲突 —— B 的最大成本，而非存储**：现 `stream()` 每轮以 `toMsgs(loadHistory(sid))` **重发全量重建的文本历史**；B 启用原生 `AgentState` 后，同一会话存在**两个历史事实源**，同时注入会造成上下文重复（直接体现为 **input token 翻倍**）、工具消息配对错乱、恢复点错位。后果：**改调用契约的工作量大于接 Redis**，必须废弃 `toMsgs` 重建、明确 AgentScope state 为唯一运行态，并处理存量会话迁移/兼容。→ 分期中新增** P4「会话事实源收敛」**作为 B 的准入前置。
2. **门禁位置必须迁移**：`approvalGate` 当前在工具执行层返回 `ToolResultBlock.suspended(...)`（字节码证实 `ToolSuspendException` 由 `ToolExecutor/ReflectiveFunctionTool` 处理），而 B 要求由权限层（`permissionContext`/`PermissionMode`）在**执行前**发 `REQUIRE_USER_CONFIRM`。两套机制**必须互斥**，否则出现「权限层已放行、工具层又挂起」的双门禁死锁。
3. **B 也省不下两项必修**：① `agent_approval` 无 insert 的 P0 缺陷与路线无关，A/B 都绕不开；② 续跑结果回推仍须改 SSE 生命周期（4.5），框架不会替你推给已断开的 HTTP 连接。
4. **实例亲和与缓存**：`harnessCache` 按 `def.getName()` 共享 `HarnessAgent` 实例，而 state 按 sessionId 分区——单实例内存缓存与多实例水平扩展下，**必须 Redis + 显式 `saveAgentState`**，否则审批后空恢复。
5. **注释与行为不一致**（见 4.2 步骤 1）：现状很可能已在写本地磁盘 state，容器重建即丢；不先核清此项就接 Redis 会出现**双存储叠加**。

**建议顺序**：先以 **A 修复可用性与审批闭环**（同时补 P0 写入链路），待 **P4 会话事实源收敛**完成后，再评估 B 是否仍需落地——若 A 的回注保真度已满足业务，B 可只保留 `PermissionRule` 免重复审批与多工具链保真两项增量收益，按需开启。

---

## 五、风险与注意事项

1. **Token 语义差异**：DashScope/OpenAI/Ollama 对 `cachedTokens`、`total` 口径不同，以各供应商 `ChatUsage.getTotalTokens()` 为权威，避免 `input+output` 自算偏差。
2. **usage 可能缺失**：部分本地模型（Ollama）或异常中断可能不回传 usage → 记 0 并降级估算（可选 jtokkit 本地分词兜底），不得 NPE。
3. **异步事件落库（v2 已确认）**：`AgentEventPublisher` 实测为**同步分发**，计量/审批监听器会直接拖慢 SSE 尾包——故**异步化提为分期 P1 前置项**（先于计量接入），监听器内异常必须静默吞掉（现审计已 try-catch，沿用）。
4. **续跑上下文保真**（A 路线）：回注的历史为「重建」，多工具链场景模型可能重复调用 → 需在执行前校验 `agent_approval.resumed`，并约束 `maxSteps`。
5. **状态双轨冲突**（B 路线，v2 上调为主要成本）：AgentScope state 与 `AgentSessionManager`(Redis) 与 `ai_chat_message`(DB) 三处会话态需单一事实源；**不得在未收敛 `toMsgs` 的情况下先接 `stateStore`**（会造成 input token 翻倍与配对错乱，见 4.7）。建议 B 落地后 AgentScope 为运行态、DB 为展示态、`AgentSessionManager` 退役。
6. **安全**：`tool_args`/`tool_result` 落库前脱敏（复用 `AiModelCipher` 思路或字段掩码）；审批与续跑强制权限校验与租户隔离，防越权触发敏感工具。
7. **数据一致性**：`agent_approval` 与 Redis 可能不一致（Redis 过期而 DB 待审），以 DB 为审批事实源、Redis 为 fast-path，过期由 DB 定时对账。
8. **聚合表唯一键含 `is_delete`（v2 新增）**：`agent_token_usage` upsert 行不得软删，否则多行撞键。
9. **注释与行为不一致（v2 新增）**：`DefaultAgentTemplate` 类注释声称关闭内部会话持久化，但 `buildHarness` 从未调用 `disableSessionPersistence()`——**文档不可信，以运行期实测为准**；B 开工前先跑一次并检查本地 state 目录。

---

## 六、分期与优先级（v2 重排）

> 重排依据：2.3 与 4.7 的可行性实测。**与 v1 的两处关键变化**：
> ① 新增 **P1 事件发布异步化**并置于 Token 计量**之前**——实测发布器为同步分发，若先做计量，每轮对话新增 2 次 DB 写会直接阻塞 SSE；
> ② 新增 **P4 会话事实源收敛**作为路线 B 的**强制准入门**，并明确其工作量大于「接 Redis」本身。成本定价（原 P3）后置于 P4，因其价值低于打通闭环。

| 阶段 | 内容 | 价值 | 可行性判定 |
|---|---|---|---|
| **P0** | 补 `agent_approval` 写入链路（insert + 权限注解） | 修复审批功能**事实不可用**的致命缺陷；**A/B 均前置，路线无关** | 高；最小改动 |
| **P1**（v2 新增前置） | `AgentEventPublisher` 异步分发改造 | 解除监听器对 SSE 尾包的阻塞，为后续所有落库监听器铺路 | 高；单一类 |
| **P2** | Token 采集（`MODEL_CALL_END` 累加 + **两出口均生效**）+ 审计真实值 + `agent_token_usage` 日聚合 + 会话累计 | 用量可见、可监控 | 高；证据链完整（2.3） |
| **P3** | 审批续跑**路线 A**（代理执行 + 回注 + SSE 回推）+ 幂等/过期/越权治理 | 审批闭环真正可用 | 高；改造集中业务层 |
| **P4**（v2 新增门禁） | **会话事实源收敛**：先运行期核实 `disableSessionPersistence` 现状→废弃 `toMsgs` 全量重发，定 AgentScope state 为唯一运行态、DB 为展示态 | 解除 B 的**最大阻塞**；副带收益：降 input token、修多轮上下文保真 | 中；需存量会话兼容 |
| **P5** | 成本定价（`ai_model` 单价 + cost 聚合）+ 会员配额联动 | 成本上限 | 高；`ai_model` 无单价字段，不先通计量链路无法定价 |
| **P6** | 审批续跑**路线 B**（原生 confirm + `RedissonAgentStateStore` + 门禁位置迁移） | 终态保真续跑 + `PermissionRule` 免重复审批 | 中高；**零新增依赖**但涉 starter 契约层 |

**为何不把 B 提前**：B 的不可替代收益只有两项（多工具链推理保真、免重复审批规则），而 P0+P3 已能交付「审批能看见、批准后真执行、用户能收到答复」的完整业务闭环；B 的主要成本在 P4 会话契约重构，与路线选择无关，属**无论选哪条路都该还的债**。

### 6.1 v1 → v2 分期映射

| v1 | v2 | 变化说明 |
|---|---|---|
| P0 | P0 | 不变（仍最紧急） |
| — | **P1** | 新增：异步化提前（实测同步分发） |
| P1 | P2 | Token 计量后移一位，前置依赖 P1 |
| P2 | P3 | 路线 A，位置基本不变 |
| — | **P4** | 新增：会话事实源收敛（B 准入门） |
| P3 | P5 | 成本/配额后置，价值低于闭环打通 |
| P4 | P6 | 路线 B，成本判定由「中」上调为「中高」 |

### 6.2 本次落地记录（P1 + P2 + P5 方案①，续跑未做）

已按本章分期实施 **P1 / P2 / P5（成本取方案①：`ai_model` 单价）**，P0/P3/P4/P6（审批续跑相关）本轮明确不做。

| 分期 | 落地内容 |
|---|---|
| P1 | `AgentEventPublisher` 改异步分发（自持线程池，`event-async/event-pool-size/event-queue-capacity`，队列饱和退化同步不丢事件），`AutoCloseable` 随容器优雅关闭 |
| P2 | 新增 `TokenUsageAccumulator`；`DefaultAgentTemplate.mapEvent` 累加 `MODEL_CALL_END`，`AGENT_END` 与 `concatWith` **两出口共用同一累加器**；`AgentFinishedEvent` 增 `totalTokens/cachedTokens/model`，`AgentReply` 增 `totalTokens`；新增 `AgentUsageEventListener` 做日聚合；`AgentChatService` 回填 `ai_chat_message.token_usage`(JSON) 与 `ai_chat_session.token_cost`；`AgentAuditEventListener` 读真实总量 |
| P5 | `sql/22_ai_model_price.sql` 为 `ai_model` 增 `input_price/output_price DECIMAL(12,6)`；新增 `ModelPricing`；单价经 `AiModelEntity → ModelConfig → AgentModelResolver.resolvePricing` 随动态池缓存，聚合时**不再回查 DB**；管理端模型表单/列表可录入与查看单价 |

**与 3.x 原设计的三处偏差（均为取证后的更优选择）**：

1. 聚合写入未采用 `INSERT ... ON DUPLICATE KEY UPDATE`——项目**无 XML mapper 也无注解 SQL 先例**，改走纯 Flex API：`UpdateChain.setRaw` 数据库侧原子累加，影响 0 行则 insert，撞唯一键再回退累加（聚合行不得软删，`is_delete` 参与唯一键）。
2. traceId 未由监听器直接读 SkyWalking——异步线程读会退化为 `N/A`，故新增 `AgentTraceIdSupplier` SPI 在**发布线程**采集写入 `AgentEvent.traceId`，SkyWalking 实现落在 `zhush-module-ai`，starter 零链路依赖。
3. 事件出口补充：`chat()` 同步路径此前**不产出** `FINISHED`（故完全不计入聚合），本次补发；`callOnce()` 不补（`sessionId` 为 null 会撞审计约束）。

**报表入口（后续补齐）**：`agent_token_usage` 此前只写不读，故补上只读查询链路——`AgentUsageQuery` / `AgentUsageVO` / `AgentUsageSummaryVO` / `AgentUsageTrendVO` + `AgentUsageService`（分页 + 同筛选条件合计与趋势）+ `AgentUsageController`（`/admin/ai/agent/usage/{page,summary,trend}`，权限 `ai:usage:list`）+ `sql/23_ai_usage_menu.sql`（菜单 3116 挂 3010）+ 前端 `views/agent/usage`（4 张指标卡 + 每日趋势图 + 明细表）。合计与趋势选择在内存中累加/分组而非自定义 SQL：聚合表一行 =「一天 × 一租户 × 一智能体」，量级有限；又因全局 Long→String 序列化，Token 类字段前端按字符串接收后 `Number()` 兜底格式化。趋势图为 Token 堆叠柱（左轴）+ 成本折线（右轴），**仅在同时传入起止日期且区间 ≤ 366 天时对无量日期补 0**，否则只返回实际有量的日期，避免无界补零。

**同期修正**：4 套 `application-*.yaml` 的 `agentscope.session: { store, ttl }` 与 `AgentScopeProperties` 的扁平属性 `sessionStore/sessionTtl` 不匹配（Binder 静默忽略，一直只吃默认值），已改为 `session-store` / `session-ttl`；`AgentScopeProperties` 注释示例与两份 AgentScope 文档的配置示例同步纠正。

---

## 七、验收标准

- **Token**：一次多步对话后，`agent_audit_log.token_usage>0` 且与模型控制台计费吻合；`agent_token_usage` 出现当日聚合行且随请求递增；`ai_chat_message.token_usage`(JSON)、`ai_chat_session.token_cost` 非 0；（P5 后）`cost` 按单价正确。
- **审批（A）**：命中敏感工具 → `agent_approval` 出现 status=0 待审记录且管理端列表可见 → approve 后**被挂起工具真实执行一次**且用户会话收到续跑最终答复 → 重复 approve 不重复执行（幂等）→ reject 不执行并回注驳回说明 → 过期不可续跑。
- **审批（B）**：在 A 验收基础上，续跑经 AgentScope state 恢复，多工具链上下文保真，`USER_CONFIRM_RESULT` 事件可见；**且开启后 `inputTokens` 不得因历史双注入而陡升（P4 收敛效果的负回归验收，v2 新增）**。
- **前置项（P1，v2 新增）**：监听器内休眠 1s 时，SSE 文本包推送不受影响（证明分发已异步）。
- 全程：typecheck/lint 通过；无 Redis 降级路径不报错；越权审批被 `@SaCheckPermission` 拦截。

---

## 八、结论

1. Token 计量与审批续跑**都无需自造底层能力**——AgentScope 2.0.2 已提供 `ChatUsage`/`MODEL_CALL_END` 与 `REQUIRE_USER_CONFIRM`/`ConfirmResult`/`RedissonAgentStateStore`/`enablePendingToolRecovery`，缺口全在**业务侧未接线**；且 `agentscope-extensions-redis` **已在 pom 中闲置**，路线 B 零新增依赖（v2 实测）。
2. **最紧急的不是计量而是审批**：`agent_approval` 无写入导致审批链路当前**事实不可用**，应列 P0 优先修复；该缺陷与路线选择无关，A/B 都绕不开。
3. **计量走「累加 MODEL_CALL_END + 事件驱动聚合 upsert」**，但必须先做 **P1 事件发布异步化**（实测同步分发，否则新增落库直接阻塞 SSE 尾包），且 `AGENT_END` 与 `concatWith` **两个出口都要用累加值**，漏一个即恒为 0；成本与配额作为增量项后置（P5）。
4. **续跑走「A 落地、P4 收敛、B 演进」**：B 的**技术前提 100% 具备**，但真实成本不在存储，而在**会话契约双轨收敛**（`toMsgs` 全量重发 vs 原生 `AgentState`）与**门禁位置迁移**（工具层 `suspended` → 权限层 confirm）。因此新增 **P4 作为 B 的强制准入门**：先核实 `disableSessionPersistence` 现状，再废弃 `toMsgs` 重建，最后才接 Redis 状态存储。
5. 四条底线：**异步落库不阻塞 SSE**、**多来源 usage 缺失兜底**、**审批幂等/越权/租户隔离**、**会话状态单一事实源（v2 新增）**。
6. **方法论提醒（v2）**：注释与文档不可作为现状依据——本方案 v1 对 `disableSessionPersistence` 的表述即源于注释而非代码，实测与注释相反；关键假设一律以代码/字节码/运行期取证为准。
