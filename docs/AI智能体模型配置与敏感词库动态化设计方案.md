# AI 智能体模型配置与敏感词库动态化设计方案

> 关联文档：
> - 《AgentScope 2.0 实现总结与使用指南》（zhush-boot4-parent/docs/AgentScope2.0实现总结与使用指南.md）
> - 《AgentScope 2.0 整合方案》（zhush-boot4-parent/docs/AgentScope2.0整合方案.md）
> - 《AI 智能体数据表设计方案》（docs/AI智能体数据表设计方案.md）

## 一、背景与目标

当前 `zhush.agentscope.models`（多供应商模型）与 `zhush.agentscope.safety.sensitive-keywords`（敏感关键词）均固化在 `application-dev.yaml`：

- 模型新增/启停需改配置并重启，无法在管理端列表操作；
- 敏感词改增需改配置并重启，无法动态维护；
- 现有模型配置无「能力类型」字段，无法区分文本/图片/视频生成能力。

### 目标

1. **模型配置库表动态化**：模型信息落库，列表中启停、增删改；运行态与前端下拉实时从库表读取；支持 `text / image / video / audio` 能力标记，并预留图片/视频生成落地路径。
2. **AI 敏感词库表动态化**：敏感词落库，管理端增删改、启停；审批门禁实时从库表（带缓存）读取，替代 yaml 硬编码。

---

## 二、现状分析（关键点）

### 2.1 模型链路

- 配置：`AgentScopeProperties.models`（`Map<String, ModelProperties>`），`ModelProperties` 含 `type / apiKey / baseUrl / model / options`。
- 消费：
  - `AgentManageService.models()`：遍历 `properties.getModels()` 组装 `AgentModelVO`（code/type/model）供前端下拉；
  - `DefaultAgentTemplate.resolveModel()`：按 Agent 的 `model` 在 `properties.getModels()` 查找，找不到用 `defaultModel`；
  - `DefaultModelFactory.create(props)`：按 `type` 分发 `DashScopeModelProvider / OpenAIModelProvider / OllamaModelProvider`。

### 2.2 敏感词链路

- 配置：`AgentScopeProperties.safety.sensitiveKeywords`（`List<String>`）。
- 消费：`DefaultAgentTemplate.approvalGate()` 用 `safety.getSensitiveKeywords().stream().anyMatch(k -> toolName.contains(k))` 判断工具是否命中敏感词，命中则走人工审批。

### 2.3 底层能力边界（重要）

经 `javap` 验证 AgentScope 2.0 `io.agentscope.core.model.Model`：

```java
public interface Model {
  Flux<ChatResponse> stream(List<Msg>, List<ToolSchema>, GenerateOptions);
  String getModelName();
}
```

**仅文本对话**，无原生的图片/视频生成方法。因此「支持图片、视频生成」采用以下落地路径（见 §5.3）：

- 模型配置表新增 `capability（能力类型）` 标记；
- 生成能力通过「多模态/生成类 `@AgentTool` 工具」或「自定义生成 ModelProvider（扩展 `Model` SPI）」落地；
- 本期以「能力标记 + 生成类工具」为主，原生生成 Provider 作为扩展点。

---

## 三、总体设计

新增 2 张表：`ai_model`（模型配置）、`ai_sensitive_word`（敏感词库）。

```
┌───────────────────────────── 管理端 前端 ─────────────────────────────┐
│  模型管理页(ai/model)   敏感词库页(ai/sensitive)                       │
│  列表启停 / 增删改       列表启停 / 增删改                              │
└──────────────┬──────────────────────────┬────────────────────────────┘
               │ REST                       │ REST
┌──────────────▼──────────────┐  ┌─────────▼──────────────────────────┐
│  zhush-module-ai           │  │  zhush-module-ai                    │
│  AiModelService(CRUD/启停) │  │  AiSensitiveWordService(CRUD/启停)  │
│  提供模型下拉(启用态)       │  │  提供审批门禁查询(带缓存)           │
└──────────────┬──────────────┘  └─────────┬──────────────────────────┘
               │ 运行态读取                 │ 审批门禁读取(缓存)
┌──────────────▼───────────────────────────▼──────────────────────────┐
│  zhush-spring-boot-starter-agentscope                                 │
│  launch: AgentScopeProperties(yml 兜底) + 动态模型/敏感词加载器        │
│  AgentModelResolver / SensitiveWordResolver(定时/事件刷新)            │
│  DefaultAgentTemplate.resolveModel() → 动态模型池                     │
│  DefaultAgentTemplate.approvalGate()  → 动态敏感词                    │
└──────────────────────────────────────────────────────────────────────┘
```

**兼容策略**：启动时若库表无数据或未启用（`agentscope.dynamic-config=false`），回退 yaml 配置，保证平滑迁移。

---

## 四、模型动态化设计

### 4.1 数据表 `ai_model`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键（雪花） |
| code | VARCHAR(64) | 逻辑名（唯一，如 dashscope-qwen-max），Agent 定义 `model` 字段引用 |
| name | VARCHAR(128) | 展示名（如 通义千问-Max） |
| type | VARCHAR(32) | 供应商标识：dashscope / openai / ollama |
| api_key | VARCHAR(512) | API Key（脱敏存储，见 §6.1） |
| base_url | VARCHAR(256) | 服务地址（openai 兼容服务） |
| model | VARCHAR(64) | 实际模型名（如 qwen-max、wanx2.1-t2i） |
| capability | VARCHAR(16) | 能力类型：text / image / video / audio（默认 text） |
| is_default | TINYINT | 是否默认模型：0-否 1-是 |
| priority | INT | 优先级（下拉排序） |
| status | TINYINT | 状态：0-停用 1-启用 |
| options | JSON | 透传参数（温度、top_p、尺寸等） |
| version | INT | 版本号（配置变更自增） |
| remark | VARCHAR(512) | 备注 |
| create_by / create_time / update_by / update_time / is_delete | — | 通用审计字段（对齐 `BaseEntity`） |

索引：`UNIQUE KEY uk_code(code, is_delete)`、`KEY idx_status_capability(status, capability)`、`KEY idx_is_default(is_default)`。

### 4.2 后端改造（zhush-module-ai）

新增：

| 层 | 文件 | 说明 |
| --- | --- | --- |
| entity | `AiModelEntity` | 对齐表结构，继承 `BaseEntity` |
| mapper | `AiModelMapper` | `BaseMapperX`，含 `selectEnabled()`（status=1） |
| param | `AiModelParam` | 保存入参 |
| query | `AiModelQuery` | 分页/筛选（type/capability/status/code） |
| vo | `AiModelVO` | 列表/下拉视图（含脱敏后的 apiKeyMasked） |
| service | `AiModelService` | CRUD、启停、`listEnabled()`、`defaultModel()`、变更后触发缓存刷新 |
| controller | `AiModelController` | `/admin/ai/model/page|list|{id}|POST|PUT|DELETE/{id}|toggle` |

`AgentManageService.models()` 改为：优先读 `AiModelService.listEnabled()`，库表无数据时回退 `properties.getModels()`。

### 4.3 starter 改造（动态模型加载）

`AgentScopeProperties` 保留 yaml；新增 `zhush.agentscope.dynamic-config`（默认 true）开关。

- 新增 `AgentModelResolver`（starter）：持有 `Supplier<List<ModelConfig>>`（由 module-ai 注入动态加载器），解析 Agent 的 `model` → 完整 `ModelProperties`（含 apiKey/baseUrl/capability）。缓存即可。
- `DefaultAgentTemplate.resolveModel()` 改为：优先动态模型池，其次 yaml，最后 `defaultModel`。
- `DefaultModelFactory` 支持按 `capability` 区分文本/图片/视频生成 provider（见 §5.3）。

### 4.4 前端（vue-vben-admin）

- 新增 `api/ai/model.ts`：`getModelPage / getModelList / createModel / updateModel / deleteModel / toggleModel`。
- 新增 `views/ai/model/index.vue` + `data.ts` + `modules/form.vue`：模型管理列表（含启停 Switch、能力类型 Tag），遵循既有列表规范。
- 调整 `views/agent/definition/modules/form.vue`：模型下拉改为读 `getModelList()`（仅启用态）。
- `api/ai/types.ts`：新增 `AiModelVO / AiModelParam`；`AgentModelVO` 增 `capability`。

---

## 五、敏感词库动态化设计

### 5.1 数据表 `ai_sensitive_word`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键（雪花） |
| word | VARCHAR(64) | 敏感词（唯一） |
| match_type | VARCHAR(16) | 匹配类型：tool_name-工具名命中 content-内容命中 both-两者 |
| group_code | VARCHAR(32) | 业务分组（默认 common） |
| level | TINYINT | 级别：1-提示 2-审批 3-拦截 |
| status | TINYINT | 状态：0-停用 1-启用 |
| remark | VARCHAR(512) | 备注 |
| create_by / create_time / update_by / update_time / is_delete | — | 通用审计字段 |

索引：`UNIQUE KEY uk_word(word, is_delete)`、`KEY idx_status_level(status, level)`。

### 5.2 后端改造（zhush-module-ai）

新增：

| 层 | 文件 | 说明 |
| --- | --- | --- |
| entity | `AiSensitiveWordEntity` | 对齐表结构 |
| mapper | `AiSensitiveWordMapper` | `BaseMapperX`，含 `selectEnabled()` |
| param | `AiSensitiveWordParam` | 保存入参 |
| query | `AiSensitiveWordQuery` | 分页/筛选（word/level/status/groupCode） |
| vo | `AiSensitiveWordVO` | 列表视图 |
| service | `AiSensitiveWordService` | CRUD、启停；提供 `enabledWords()`（带本地缓存，变更/定时刷新） |
| controller | `AiSensitiveWordController` | `/admin/ai/sensitive/page|{id}|POST|PUT|DELETE/{id}|toggle` |

### 5.3 starter 改造（审批门禁动态读取）

- 新增 `SensitiveWordResolver`（starter）：持有 `Supplier<List<String>>`（由 module-ai 注入 `enabledWords()`）。
- `AgentScopeProperties.safety.sensitiveKeywords` 保留为兜底。
- `DefaultAgentTemplate.approvalGate()` 改为：`sensitiveWordResolver.match(toolName, content)` 命中（优先动态库，库空回退 yaml）。`match_type=content` 时对 `arguments` 内容做匹配。

---

## 六、关键设计点

### 6.1 API Key 脱敏

遵循已确认的「API Key 脱敏安全策略」：
- `api_key` 落库前强制脱敏？——**不**，`api_key` 需完整存储供运行时调用；脱敏针对 **日志/审计**（tool_args/tool_result 中的 `sk-`/`api_key`/`secret`/`token` 落日志前替换为 `***`），与现有审计监听器 `AgentAuditEventListener` 一致。
- 前端列表/查询均返回 `apiKeyMasked`（如 `sk-****...`），不回传完整明文；仅编辑时回填解密或留空占位。

### 6.2 缓存与一致性

- 模型与敏感词均采用**本地内存缓存**（`ConcurrentHashMap` + 版本号），由 service 在增删改/启停后主动 `refresh()`，避免每次 DB 查询。
- 敏感词同时支持可选定时刷新（如每 5 分钟）兜底，防止多实例不一致。
- 启动时：`db-cache` 预热加载启用态数据。

### 6.3 图片/视频生成落地路径（本期以能力标记为主）

- **能力标记**：`ai_model.capability` 支持 `text / image / video / audio`，前端以 `Tag` 展示，下拉可筛选。
- **生成调用**：
  - 优先走「生成类 `@AgentTool` 工具」（如 add 到 `UnionAgentTools`：`generateImage / generateVideo`，调用对应供应商生成 API，返回 URL）；
  - 预留 `GenerationModelProvider` 扩展点：实现 AgentScope `Model` SPI 或独立的生成接口，后续可接入原生文生图/文生视频。
- `DefaultModelFactory.create()`：`capability=image/video` 时不适配文本 `Model` 接口，而是交给「生成工具 / 独立生成适配器」消费；文本模型保持原有 `stream()` 链路。

---

## 七、SQL 脚本（建议新增 `sql/14_agent_model_sensitive.sql`）

```sql
-- 内置于 sql/14_agent_model_sensitive.sql，参照 13_agent_ai.sql 风格
CREATE TABLE `ai_model` ( ... 见 4.1 );   -- utf8mb4，主键雪花
CREATE TABLE `ai_sensitive_word` ( ... 见 5.1 );

INSERT INTO `ai_model`(id, code, name, type, model, capability, is_default, status) VALUES
  (91001, 'qwen-max', '通义千问-Max', 'dashscope', 'qwen-max', 'text', 1, 1),
  (91002, 'deepseek-chat', 'DeepSeek-Chat', 'openai', 'deepseek-chat', 'text', 0, 1),
  (91003, 'wanx2.1-t2i', '通义万相-文生图', 'dashscope', 'wanx2.1-t2i', 'image', 0, 1);

INSERT INTO `ai_sensitive_word`(id, word, match_type, level, status) VALUES
  (92001, '删除', 'tool_name', 2, 1),
  (92002, '转账', 'tool_name', 2, 1),
  (92003, '退款', 'tool_name', 2, 1);
```

> 菜单/权限 SQL 参照 `13_agent_ai.sql` 约定新增「模型管理」「敏感词库」目录与按钮权限，并显式写入 `sys_role_menu`。

---

## 八、接口清单

### 模型管理 `/admin/ai/model`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /page | 分页（type/capability/status/code） |
| GET | /list | 启用态列表（下拉） |
| GET | /{id} | 详情（apiKeyMasked） |
| POST | / | 新增 |
| PUT | /{id} | 编辑 |
| DELETE | /{id} | 删除 |
| PUT | /{id}/toggle | 启停 |

### 敏感词库 `/admin/ai/sensitive`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /page | 分页（word/level/status/groupCode） |
| GET | /{id} | 详情 |
| POST | / | 新增 |
| PUT | /{id} | 编辑 |
| DELETE | /{id} | 删除 |
| PUT | /{id}/toggle | 启停 |

---

## 九、前端页面

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 模型管理 | `views/ai/model/index.vue` | 列表分页 + 启停 Switch + 能力类型 Tag + 新增/编辑/删除 |
| 模型编辑 | `views/ai/model/modules/form.vue` | 表单（type/capability/baseUrl/apiKey/model/默认/优先级） |
| 敏感词库 | `views/ai/sensitive/index.vue` | 列表分页 + 启停 + 级别/匹配类型 Tag + 增删改 |

均遵循既有规范：列表无标题、含序号列、文案统一、新增按钮在左、刷新在右。

---

## 十、涉及改动文件清单

### 后端

- **新表**：`sql/14_agent_model_sensitive.sql`
- **zhush-module-ai（新增）**：`AiModelEntity / AiModelMapper / AiModelParam / AiModelQuery / AiModelVO / AiModelService / AiModelController`；`AiSensitiveWordEntity / ... / AiSensitiveWordController`
- **zhush-module-ai（修改）**：`AgentManageService.models()`（回退逻辑）、`AgentChatService`（若需记录 capability）
- **starter（新增）**：`AgentModelResolver`、`SensitiveWordResolver`、`config/AgentScopeDynamicConfig`
- **starter（修改）**：`AgentScopeProperties`、`AgentScopeAutoConfiguration`、`DefaultAgentTemplate.resolveModel()`、`DefaultAgentTemplate.approvalGate()`、`DefaultModelFactory`

### 前端

- **新增**：`api/ai/model.ts`、`api/ai/sensitive.ts`、`views/ai/model/*`、`views/ai/sensitive/*`
- **修改**：`api/ai/types.ts`（AiModelVO/AiModelParam、AgentModelVO.capability）、`views/agent/definition/modules/form.vue`（下拉改动态）

---

## 十一、风险与注意事项

1. **兼容回退**：库表为空或 `dynamic-config=false` 时必须回退 yaml，避免升级后无法启动。
2. **API Key 安全**：`api_key` 完整存储供运行时调用，但列表/详情仅返回 `apiKeyMasked`；日志脱敏沿用现有审计监听器。
3. **能力区分**：图片/视频生成本期以「能力标记 + 生成类工具」落地；底层 AgentScope `Model` 接口无原生生成，需预留扩展。
4. **缓存一致**：多实例部署下敏感词/模型变更需广播刷新（可选订阅 Reids pub/sub 或定时兜底）。
5. **菜单授权**：新增目录/按钮权限需显式写入 `sys_role_menu`（已确认决策）。

---

## 十二、验收标准

1. 模型管理列表可启停、增删改，前端 Agent 表单下拉仅显示启用模型；
2. 模型变更后无需重启即生效（缓存刷新）；
3. 敏感词库增删改/启停后，审批门禁立即按新词生效；
4. `capability=image` 的模型在列表以「图片生成」Tag 展示，可关联到生成类工具；
5. 库表为空时系统仍可基于 yaml 配置正常启动（兼容回退）。
