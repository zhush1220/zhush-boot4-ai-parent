# AgentScope 2.0 整合实现总结与使用指南

> 关联文档：
> - 《AgentScope 2.0 整合方案》（zhush-boot4-parent/docs/AgentScope2.0整合方案.md）
> - 《AgentScope 2.0 整合开发计划》（docs/AgentScope2.0整合开发计划.md）
> - 《AI 智能体前端页面设计方案》（docs/AI智能体前端页面设计方案.md）
> - 《AI 智能体数据表设计方案》（docs/AI智能体数据表设计方案.md）

本文件基于已合并的代码，总结 AgentScope Java 2.0 自建 starter 的落地实现，说明如何使用、如何扩展，作为开发与联调的依据。

---

## 一、实现概览

以自建 Spring Boot Starter（`zhush-spring-boot-starter-agentscope`）方式整合 AgentScope Java 2.0（`io.agentscope:agentscope-harness:2.0.2`），为 `zhush-module-ai`、`zhush-module-union` 提供 **多模型、多轮会话、流式、工具调用、人工审批、审计、多租户** 能力；前端（vue-vben-admin web-antdv-next）提供「AI 智能助手」与「智能体管理」两大模块。

### 已实现范围（里程碑 M1–M4）

| 层级 | 内容 | 状态 |
| --- | --- | --- |
| 后端 M1 | AgentScope starter 骨架（配置、契约、工具、会话、事件、审批、上下文） | 完成 |
| 后端 M2 | `zhush-module-ai` 实体化（7 表落库 + 管理端接口 + 流式接口） | 完成 |
| 后端 M3 | `zhush-module-union` 接入（5 个业务 Agent + 业务工具） | 完成 |
| 后端 M4 | `zhush-server` 配置段 + 依赖 | 完成 |
| 前端 | API 层 + `use-chat-stream` 流式状态机 + 6 个页面 | 完成 |

编译验证：后端各模块 `mvn compile` 均 `BUILD SUCCESS`；前端 `vue-tsc --noEmit` 零报错。

---

## 二、架构说明

```
┌────────────────────────────────────────────────────────────┐
│  前端 vue-vben-admin (web-antdv-next)                       │
│   ├ api/ai/*        REST + SSE 封装                         │
│   ├ hooks/use-chat-stream.ts   流式状态机                   │
│   └ views/ai + views/agent      6 个页面                    │
└──────────────┬─────────────────────────────────────────────┘
               │ HTTP /admin/ai/**
┌──────────────▼─────────────────────────────────────────────┐
│  zhush-module-ai   （对外接口 + 落库）                       │
│  ├ AgentChatController     对话/流式/会话                   │
│  ├ AgentDefinition…        管理端 CRUD                      │
│  ├ AgentApproval…          审批                             │
│  ├ AgentLog…               审计                             │
│  ├ AgentTool…              工具列表                         │
│  └ 7 表 CRUD                                                │
└──────────────┬─────────────────────────────────────────────┘
               │ 依赖
┌──────────────▼─────────────────────────────────────────────┐
│  zhush-spring-boot-starter-agentscope   （自建 starter）     │
│  ├ config  AgentScopeProperties + AutoConfiguration         │
│  ├ agent   AgentDefinition/Registry/Provider/Reply          │
│  ├ tool    注解扫描 + ToolkitRegistry + 桥接                │
│  ├ session 会话存储(Redis/Memory) + 会话管理               │
│  ├ event   事件发布 + 事件监听                              │
│  ├ safety  工具审批(Redis/Null)                            │
│  ├ template DefaultAgentTemplate（编排入口）                │
│  └ context 会话上下文解析（多端 Sa-Token）                  │
└──────────────┬─────────────────────────────────────────────┘
               │ 业务声明 Provider + @AgentTool
┌──────────────▼─────────────────────────────────────────────┐
│  zhush-module-union   UnionAgentDefinitionProvider          │
│                       UnionAgentTools（5 业务工具）         │
└─────────────────────────────────────────────────────────────┘
```

关键设计点：

- **工具桥接**：业务方法使用自定义 `@AgentTool` 注解，无法被 AgentScope 原生 Toolkit 识别；starter 提供 `ReflectiveAgentTool` 将业务 `@AgentTool` 包装为 `io.agentscope.core.tool.AgentTool` 接口实现，并在容器单例就绪后通过 `AgentToolScanner` 扫描注册。
- **审批去 Redis**：`DefaultToolApprovalHandler` 以 `agentscope:approval:{approvalId}` 为 key 存审批状态，TTL 对齐会话 TTL；无 Redis 时降级 `NullToolApprovalHandler`。
- **上下文多端**：`SessionContextResolver` 抽象 + `AiSessionContextResolver`（管理端 `StpAdminUtil` / 会员端 `StpMemberUtil`）实现多租户、多端隔离。
- **SSE 传输**：后端 `Flux<AgentEvent>`（`text/event-stream`），前端用 `fetch + ReadableStream` 以携带 Authorization 头。

---

## 三、后端核心模块

### 1. Starter：`zhush-spring-boot-starter-agentscope`

位于 `zhush-framework/zhush-spring-boot-starter-agentscope/src/main/java/com/zhush/agentscope/`：

| 包 | 说明 | 关键类 |
| --- | --- | --- |
| `config` | 配置属性与自动装配 | `AgentScopeProperties`、`AgentScopeAutoConfiguration` |
| `agent` | 智能体定义与注册表 | `AgentDefinition`、`AgentRegistry`、`DefaultAgentRegistry`、`AgentDefinitionProvider`、`AgentReply` |
| `tool` | 工具扫描与注册 | `@AgentTool`、`@ToolParam`、`ToolkitRegistry`、`DefaultToolkitRegistry`、`AgentToolScanner`、`ReflectiveAgentTool` |
| `session` | 会话存储与管理 | `AgentSessionStore`、`RedisAgentSessionStore`、`MemoryAgentSessionStore`、`AgentSessionManager` |
| `event` | 事件总线 | `AgentEvent`、`AgentEventPublisher`、`AgentEventListener`、`AgentApprovalRequiredEvent`、`AgentErrorEvent` |
| `safety` | 工具审批 | `ToolApprovalHandler`、`DefaultToolApprovalHandler`、`NullToolApprovalHandler` |
| `template` | 编排入口 | `AgentTemplate`、`DefaultAgentTemplate` |
| `context` | 会话上下文 | `SessionContextResolver`、`DefaultSessionContextResolver` |
| `model` | 模型工厂 | `DefaultModelFactory` |

**功能开关**：`zhush.agentscope.enabled=true` 时装配全部 Bean。

### 2. `zhush-module-ai`（数据 + 对外）

- 数据层：`entity` / `mapper` / `service`，覆盖 7 张表 CRUD。
- 对外接口（前缀 `/admin/ai`）：
  - `/agent/chat`：同步对话
  - `/agent/stream`：流式对话（SSE）
  - `/agent/list`：智能体列表
  - `/agent/session/new`、`/agent/session/page`、`/agent/session/{id}/messages`、`/agent/session/{id}`：会话管理
  - `/agent/manage/page`、`/agent/manage/{id}`、`/agent/manage/models`：Agent 管理
  - `/agent/approval/page`、`/agent/approval/approve`、`/agent/approval/reject`：审批
  - `/agent/tool/list`：工具列表（只读）
  - `/agent/log/page`、`/agent/log/{id}`：审计日志（只读）

### 3. `zhush-module-union`（业务示例）

- `UnionAgentDefinitionProvider`：声明 5 个业务 Agent
  `union-selector`（选品推荐）/ `union-policy`（政策咨询）/ `union-copywriter`（文案生成）/ `union-compliance`（合规审核）/ `union-insight`（经营洞察），均 `groupCode=biz`、`streaming=true`、`maxSteps=6`。
- `UnionAgentTools`：7 个业务工具
  `queryHotGoods` / `queryGoodsInfo` / `calcCommission` / `queryPolicyRule` / `generateCopywriting` / `complianceCheck`（`sensitive=true`）/ `queryInsightStats`。

---

## 四、数据表

建表脚本：`sql/13_agent_ai.sql`

| 表 | 说明 |
| --- | --- |
| `agent_definition` | 智能体定义（提示词/模型/工具/步数/分组/状态） |
| `agent_tool` | 工具注册快照（只读） |
| `ai_chat_session` | 对话会话记录 |
| `ai_chat_message` | 对话消息（含思考/工具调用/Token 用量） |
| `agent_approval` | 敏感工具审批任务 |
| `agent_audit_log` | AI 审计日志（事件总线落库） |
| `agent_token_usage` | AI 用量汇总（成本监控） |

内含初始化示例数据（3 个公共 Agent）及菜单/授权 SQL（见第八节）。

---

## 五、配置说明

`zhush-server/src/main/resources/application-dev.yaml`：

```yaml
zhush:
  agentscope:
    enabled: true                 # 总开关
    default-model: qwen-max       # 默认模型
    models:                       # 多供应商模型
      dashscope: { type: dashscope, api-key: ${DASHSCOPE_API_KEY:}, model: qwen-max }
      deepseek:  { type: openai, api-key: ${DEEPSEEK_API_KEY:}, base-url: https://api.deepseek.com/v1, model: deepseek-chat }
      local:     { type: ollama, base-url: http://localhost:11434, model: qwen2.5:7b }
    session:
      store: redis                # redis / memory
      ttl: 24h
    request-timeout: 60s
    safety:
      approval-enabled: true
      sensitive-keywords: [删除, 转账, 下单, 退款]
```

> **注意**：`api-key` 使用 `${ENV}` 占位，禁止硬编码；`DASHSCOPE_API_KEY` / `DEEPSEEK_API_KEY` 需在环境或 `.env` 中配置，否则对应模型无法调用。配置变更后需重启生效。

---

## 六、前端实现

### 1. API 层（`apps/web-antdv-next/src/api/ai/`）

| 文件 | 接口 |
| --- | --- |
| `types.ts` | 前后端对齐的类型定义 |
| `agent.ts` | 智能体列表/定义 CRUD/模型列表 |
| `session.ts` | 会话分页/消息/删除 |
| `tool.ts` | 工具列表 |
| `approval.ts` | 审批分页/通过/驳回 |
| `log.ts` | 审计日志分页/详情 |
| `stream.ts` | SSE 封装（`fetchStream` + `normalizeEventType`） |

### 2. 流式状态机（`hooks/use-chat-stream.ts`）

`useChatStream({ agentName, sessionId, onMessage })` 返回 `{ messages, send, stop, streaming, currentSessionId, clear }`。事件映射：`session`/`message`/`thinking`/`tool_start`/`tool_result`/`tool_approval`/`done`/`error`。

### 3. 页面

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| AI 智能助手 | `views/ai/assistant/index.vue` | 三栏对话工作台（会话侧边栏 + 对话流 + Agent 选择器/场景胶囊） |
| 会话记录 | `views/ai/session/index.vue` | 会话列表 + 删除 |
| Agent 管理 | `views/agent/definition/index.vue` | 定义 CRUD（`modules/form.vue` + `data.ts`） |
| 工具管理 | `views/agent/tool/index.vue` | 工具只读列表 |
| 审批中心 | `views/agent/approval/index.vue` | 通过 / 改参通过 / 驳回 |
| AI 审计日志 | `views/agent/log/index.vue` | 只读分页 + 详情抽屉 |

---

## 七、如何使用

### 1. 后端启动

```powershell
# 1. 建库建表（执行一次）
#    在 MySQL 中执行 sql/13_agent_ai.sql

# 2. 配置模型 Key（可选，不配则本地 ollama 或默认模型）
#    缺省时 dashscope/deepseek 的 api-key 为空，无法调用

# 3. 编译启动
cd D:\zhush\project\back-boot\zhush-boot4-parent
mvn -pl zhush-server -am spring-boot:run
```

### 2. 前端启动

```powershell
cd D:\zhush\project\back-boot\vue-vben-admin\apps\web-antdv-next
pnpm install
pnpm dev
```

### 3. 联调路径

1. 以管理端账号登录。
2. 进入「AI 助手 → AI 智能助手」，选择一个智能体（公共或业务），输入问题发送，观察流式回答与工具卡片。
3. 触发敏感工具（如 `union-compliance` / 「删除」「转账」关键词）时，消息进入「审批挂起」，前往「智能体管理 → 审批中心」处理（通过 / 改参通过 / 驳回）。
4. 在「Agent 管理」新增/编辑 Agent，配置模型、工具、步数、分组；「工具管理」「AI 审计日志」为只读查看。

---

## 八、菜单与权限

`sql/13_agent_ai.sql` 已内置菜单与授权（幂等，可重复执行）：

- 顶级目录：`AI 助手`（id=3000）、`智能体管理`（id=3010）
- 子菜单：AI 智能助手、会话记录、Agent 管理、工具管理、审批中心、AI 审计日志
- 按钮权限：`ai:assistant`、`ai:session:list/delete`、`agent:definition:list/create/update/delete`、`agent:tool:list`、`agent:approval:list/approve/reject`、`agent:log:list`
- 授权：已显式写入 `sys_role_menu`（role_id=1 超级管理员）。

> 依据已确认决策：菜单授权需显式 `sys_role_menu` 落库，不依赖超级管理员通配。非超管角色如需访问请另加授权。

---

## 九、后续扩展

### 1. 新增业务 Agent（代码声明）

在任意模块（如 `zhush-module-union`）实现 `AgentDefinitionProvider`：

```java
@Component
public class MyAgentProvider implements AgentDefinitionProvider {
    @Override
    public List<AgentDefinition> getDefinitions() {
        return List.of(
            AgentDefinition.builder()
                .name("my-agent")
                .description("说明")
                .systemPrompt("系统提示词")
                .model("qwen-max")
                .toolNames(List.of("myTool"))
                .streaming(true)
                .maxSteps(6)
                .groupCode("biz")
                .build()
        );
    }
}
```

### 2. 新增业务工具

在任意 `@Component` 中暴露 `@AgentTool` 方法（入参用 `@ToolParam` 说明）：

```java
@AgentTool(name = "myTool", description = "说明", sensitive = true)
public String myTool(@ToolParam(name = "arg", description = "入参") String arg) {
    return "...";
}
```

`sensitive=true` 的工具调用会进入人工审批流程。

### 3. 新增模型供应商

往 `AgentScopeProperties.models` 增加条目，并在 `application-dev.yaml` 的 `models` 段配置 `type/api-key/base-url/model`。`type` 支持 `dashscope` / `openai`（兼容服务） / `ollama`。

### 4. 深度能力扩展

- **用量统计**：基于 `agent_token_usage` 表，按租户/智能体/日聚合，可对接成本上限与监控面板。
- **审批规则**：扩展 `ToolApprovalHandler` 或用 `sensitive-keywords` 命中规则，支持更细粒度的鉴权与审批人指派。
- **技能（Skill）**：`AgentDefinition` 已预留 `skills` 字段，可对接 HarnessAgent 的 Skill 扩展智能体能力。
- **事件扩展**：实现 `AgentEventListener` 接入自定义审计、告警、业务通知。
- **会员端**：接入 `StpMemberUtil`，复用 `SessionContextResolver`，实现多端会话隔离。

### 5. 前端扩展

- 新增页面遵循既有规范（列表无标题、序号列、文案统一、新增按钮在左等）。
- 新增接口在 `api/ai/` 下扩展；流水线提示词可在 `views/ai/assistant/data.ts` 的 `UNION_SCENES` 增加场景胶囊。

---

## 十、注意事项与坑点

1. `createIconifyIcon` 应从 `@vben/icons` 导入（非 `@vben/common-ui`）。
2. antdv-next 的 `Descriptions` 不支持静态 `.Item`，应单独导入 `DescriptionsItem` 组件。
3. 表格 `fixed: 'right'` 需用 `as const` 字面量类型，否则 `fixed` 被推断为 `string` 导致类型不匹配。
4. `@keydown.enter` 绑定处理器需接收 `KeyboardEvent` 参数，不能直接绑定带参数的发送函数。
5. 后端 `api-key` 一律走 `${ENV}`，避免密钥硬编码。
6. 前端流式使用 `fetch + ReadableStream`（SSE），而非 `EventSource`，以便携带鉴权头。
7. 编译命令（PowerShell）：后端 `mvn -pl <module> compile`；前端 `vue-tsc --noEmit --skipLibCheck`（工作目录 `apps/web-antdv-next`）。

---

## 十一、快速参考命令

```powershell
# 后端编译整个模块
cd D:\zhush\project\back-boot\zhush-boot4-parent
mvn -pl zhush-module-ai -am compile
mvn -pl zhush-module-union -am compile
mvn -pl zhush-server -am compile

# 前端类型检查
cd D:\zhush\project\back-boot\vue-vben-admin
node node_modules\vue-tsc\bin\vue-tsc.js --noEmit --skipLibCheck --project apps\web-antdv-next\tsconfig.json
```
