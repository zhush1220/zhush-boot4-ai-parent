# AI 模型配置与敏感词库动态化 —— 实现说明

> 对应设计方案：[AI智能体模型配置与敏感词库动态化设计方案.md](./AI智能体模型配置与敏感词库动态化设计方案.md)（v2）
> 配套文档：[AI模型配置与敏感词库动态化使用指南.md](./AI模型配置与敏感词库动态化使用指南.md)
> SQL 脚本：`sql/15_agent_model_sensitive.sql`

## 1. 实现目标

将原先固化在 `application-*.yaml` 中的两类配置迁移到数据库动态管理：

| 原配置项 | 落库表 | 管理端菜单 |
|---|---|---|
| `zhush.agentscope.models` | `ai_model` | 模型管理（3114） |
| `zhush.agentscope.safety.sensitive-keywords` | `ai_sensitive_word` | 敏感词库（3115） |

核心能力：

- 管理端列表增删改查、一键启停，**运行态实时生效**（无需重启）
- 模型支持 `text / image / video / audio` 能力类型标记
- 敏感词分级命中：**1-提示（记录日志放行）/ 2-审批（挂起待人工审批）/ 3-拦截（直接拒绝）**
- 配置变更三级刷新：本实例事件 → Redis pub/sub 广播 → 定时兜底
- 完全兼容回退：动态库关闭或空库时自动回退 yaml 配置

## 2. 数据库变更

脚本：`sql/15_agent_model_sensitive.sql`（编号 15，避开已占用的 `14_notice.sql`）

### 2.1 ai_model（模型配置）

| 字段 | 说明 |
|---|---|
| `code` | 模型编码，Agent 定义（`agent_definition.model`）引用的唯一键，创建后不可修改 |
| `name` | 模型显示名称 |
| `type` | 供应商：`dashscope` / `openai` / `ollama` |
| `model` | 实际模型名（如 `qwen-max`） |
| `api_key` | AES 加密存储；支持 `${ENV}` 环境变量占位（占位不加密，运行时解析） |
| `base_url` | 服务地址（openai 兼容服务必填） |
| `capability` | 能力类型：`text`（默认）/ `image` / `video` / `audio` |
| `is_default` | 是否默认模型（全表唯一） |
| `priority` / `status` / `remark` | 优先级 / 启停 / 备注 |

初始数据：`93001 qwen-max`、`93002 deepseek-chat`（默认）、`93003 local-qwen`（停用）。

### 2.2 ai_sensitive_word（敏感词）

| 字段 | 说明 |
|---|---|
| `word` | 敏感词（全表唯一） |
| `match_type` | 匹配对象：`tool_name` / `content` / `both`（默认） |
| `level` | 命中级别：`1` 提示 / `2` 审批 / `3` 拦截 |
| `group_code` | 分组（预留按业务域隔离） |

初始数据：`94001~94004`（删除/转账/下单/退款，与旧 yaml 词表一致，级别均为拦截）。

### 2.3 菜单与权限

- 菜单 `3114 模型管理`（按钮 31141 新增 / 31142 修改 / 31143 删除）
- 菜单 `3115 敏感词库`（按钮 31151 新增 / 31152 修改 / 31153 删除）
- 均挂载于 `3010`（AI 智能体目录），`sys_role_menu` 已授权 `role_id=1`（超级管理员）
- 权限标识：`ai:model:list/create/update/delete`、`ai:sensitive:list/create/update/delete`

## 3. starter 层实现（zhush-spring-boot-starter-agentscope）

### 3.1 新增 SPI 与解析器

| 类 | 职责 |
|---|---|
| `model/ModelConfig` | 模型配置快照（code/type/model/apiKey/baseUrl/capability/options） |
| `model/ModelConfigProvider` | SPI：由业务模块实现，提供启用态模型列表 |
| `model/AgentModelResolver` | 动态模型解析：缓存启用态模型，`resolve(code)` 动态优先、未命中返回 null 交由模板回退 |
| `safety/SensitiveWordConfig` | 敏感词配置快照（word/matchType/level） |
| `safety/SensitiveWordProvider` | SPI：提供启用态敏感词列表 |
| `safety/SensitiveWordMatch` | 匹配结果（命中词列表 + 级别常量 `LEVEL_NOTICE=1 / LEVEL_APPROVAL=2 / LEVEL_BLOCK=3`） |
| `safety/SensitiveWordResolver` | 缓存词表，`match(toolName, content)` 按 matchType 分级匹配 |
| `config/DynamicConfigRefreshCoordinator` | 刷新协调器：`refreshAll()` 触发两个 resolver 重载 + 通知缓存失效监听器；`startScheduled(interval)` 定时兜底 |

### 3.2 DefaultAgentTemplate 改造

- `resolveModel(def)`：优先 `AgentModelResolver.resolve(code)`（DB 动态池）；未命中回退 `DefaultModelFactory.resolve(code)`（yaml），并记录 warn 日志
- `approvalGate(def, rt)` 升级为**调用时动态分级检测**：
  1. 匹配到拦截级（3）敏感词 → 返回 `ToolResultBlock.error(...)` 直接拒绝
  2. 匹配到提示级（1）→ 记录 warn 日志后放行
  3. 匹配到审批级（2）或工具本身 `requiresApproval` → 且 `safety.approval-enabled=true` 时，走原有审批挂起流程（`ToolResultBlock.suspended`）
- 新增 `evictAgentCache(name)` / `evictAllAgentCache()`：`harnessCache` 中的 `HarnessAgent` 在构建时已绑定 Model 实例，模型变更后必须清缓存才能生效

### 3.3 AutoConfiguration 装配（AgentScopeAutoConfiguration）

- `agentModelResolver`：`dynamic-config.enabled=false` 时注入空 provider 列表（等价纯 yaml 模式）
- `sensitiveWordResolver`：同上
- `dynamicConfigRefreshCoordinator`：enabled 时启动定时兜底刷新
- `agentTemplate`：注入两个 resolver + coordinator，并注册 `template::evictAllAgentCache` 为缓存失效监听器
- `agentConfigRefreshListenerContainer`：`@ConditionalOnBean(RedisConnectionFactory)` + `broadcast=true` 时，订阅 Redis 频道 **`agentscope:config:refresh`**，收到消息执行 `refreshAll()`

### 3.4 配置项（AgentScopeProperties.DynamicConfig）

```yaml
zhush:
  agentscope:
    dynamic-config:
      enabled: true          # false 时完全回退 yaml 静态模式
      refresh-interval: 300s # 定时兜底刷新周期
      broadcast: true        # 多实例 Redis pub/sub 广播
    security:
      model-encrypt-key: xxx # API Key AES 加密密钥（缺省用内置默认值）
```

## 4. module-ai 层实现

### 4.1 AiModel（模型管理）

| 类 | 说明 |
|---|---|
| `entity/AiModelEntity`、`mapper/AiModelMapper` | MyBatis-Flex 数据层 |
| `param/AiModelParam`、`query/AiModelQuery`、`vo/AiModelVO` | 入参 / 查询 / 出参（VO 仅返回 `apiKeyMasked` 掩码） |
| `service/AiModelCipher` | hutool AES 加密（密钥由 `model-encrypt-key` SHA256 派生 32 字节）；`${ENV}` 占位原样存储、运行时解析；掩码规则：前 4 + `****` + 后 4 |
| `service/AiModelService` | 核心业务，规则见下 |
| `provider/DbModelConfigProvider` | 实现 starter SPI，把启用态实体映射为 `ModelConfig` |
| `controller/admin/AiModelController` | `/admin/ai/model/**`，Sa-Token `@SaCheckPermission` |

业务规则：

- 模型编码全局唯一；**编辑时 code 不可变更**（Agent 定义按 code 引用）
- 默认模型互斥：设为默认时自动清除旧默认
- 删除前 `checkNotReferenced`：存在启用态 `agent_definition.model = code` 时禁止删除
- 默认模型禁止停用（防止兜底链路断裂）
- 编辑时 `apiKey` 为空 → 保留原值
- `test(id)`：`DefaultModelFactory.create` + `model.stream(...).blockLast(20s)` 真实连通性验证
- 所有写操作完成后执行 `refreshAndBroadcast()`：本地 `refreshCoordinator.refreshAll()` + Redis `convertAndSend("agentscope:config:refresh", "refresh")`

### 4.2 AiSensitiveWord（敏感词库）

结构与 AiModel 对齐：`AiSensitiveWordEntity/Mapper/Param/Query/VO/Service/Controller` + `provider/DbSensitiveWordProvider`。

业务规则：敏感词唯一校验、启停、写操作后同样触发刷新广播。

### 4.3 Agent 定义联动

- `AgentManageService.models()`：改为**动态池优先**（`AiModelService.listEnabled()` 映射 `AgentModelVO`，含 name/capability/isDefault），动态库为空时回退 yaml `models` 配置
- `AgentModelVO` 扩展 `name` / `capability` / `isDefault` 字段

### 4.4 错误码（AiErrorCode）

| 错误码 | 含义 |
|---|---|
| 1003104 | 模型编码已存在 |
| 1003105 | 模型被 Agent 引用，不可删除 |
| 1003106 | 默认模型不可停用 |
| 2003004 | 模型连接测试失败 |
| 1003301 | 敏感词不存在 |
| 1003302 | 敏感词已存在 |

## 5. 前端实现（vue-vben-admin / web-antdv-next）

| 文件 | 说明 |
|---|---|
| `src/api/ai/types.ts` | 新增 `AiModelVO/AiModelParam/AiSensitiveWordVO/AiSensitiveWordParam`，`AgentModelVO` 扩展 name/capability/isDefault |
| `src/api/ai/model.ts` | 模型 CRUD / toggle / test |
| `src/api/ai/sensitive.ts` | 敏感词 CRUD / toggle |
| `src/views/agent/model/` | 模型管理页：查询（编码/供应商/能力/状态）、能力类型彩色 Tag、默认徽标、API Key 掩码列、Switch 直切启停、连接测试、新增/编辑弹窗（编辑时 code 禁用、apiKey 留空保留原值） |
| `src/views/agent/sensitive/` | 敏感词库页：级别 Tag（提示蓝/审批橙/拦截红）、匹配对象、Switch 启停、新增/编辑弹窗 |
| `src/views/agent/definition/data.ts` | Agent 表单模型下拉 label 改为 `模型名称（编码）`，数据源即动态模型池 |

## 6. 生效链路（变更如何实时生效）

```
管理端操作（增/改/删/启停）
   └─> AiModelService / AiSensitiveWordService 落库
        └─> refreshAndBroadcast()
             ├─ L1 本实例：coordinator.refreshAll()
             │     ├─ AgentModelResolver 重载启用态模型缓存
             │     ├─ SensitiveWordResolver 重载词表缓存
             │     └─ 触发 evictAllAgentCache()（清除已绑定旧模型的 HarnessAgent）
             ├─ L2 其他实例：Redis pub/sub 频道 agentscope:config:refresh → 各实例 refreshAll()
             └─ L3 兜底：定时任务每 refresh-interval（默认 300s）全量刷新
```

敏感词无需清缓存（审批门禁每次工具调用时实时查询 resolver）；模型因 `HarnessAgent` 构建时绑定，必须走缓存失效。

## 7. 验证结果

- 后端：`mvn install -DskipTests -pl zhush-framework/zhush-spring-boot-starter-agentscope,zhush-module-ai,zhush-server` 全链路编译通过
- 前端：`pnpm --filter @vben/web-antdv-next typecheck`（vue-tsc）通过

## 8. 风险与回退

| 场景 | 行为 |
|---|---|
| `dynamic-config.enabled=false` | resolver 注入空 provider，完全等价旧版 yaml 模式 |
| 表未建 / 启动时表为空 | provider 加载异常被 try/catch 吞掉并回退 yaml，不影响启动 |
| yaml 中 models 被删但 DB 有数据 | 动态池命中即用，不受影响 |
| Redis 广播关闭 | 退化为「本实例即时 + 定时兜底」最终一致 |
