# AI 智能体模型配置与敏感词库动态化设计方案（v2 · 深度优化）

> 版本：v2（2026-09-01），v1 → v2 主要增强：Agent 缓存失效机制、模型启停校验、敏感词分级落地、API Key 加密存储、三级缓存刷新、yaml→DB 迁移。
>
> 关联文档：
> - 《AgentScope 2.0 实现总结与使用指南》（zhush-boot4-parent/docs/AgentScope2.0实现总结与使用指南.md）
> - 《AgentScope 2.0 整合方案》（zhush-boot4-parent/docs/AgentScope2.0整合方案.md）
> - 《AI 智能体数据表设计方案》（docs/AI智能体数据表设计方案.md）

## 一、背景与目标

### 1.1 现状痛点

当前模型与敏感词均固化在 `zhush-server/src/main/resources/application-dev.yaml`：

```yaml
zhush:
  agentscope:
    default-model: deepseek-chat
    models:
      dashscope: { type: dashscope, api-key: "${DASHSCOPE_API_KEY:}", model: qwen-max }
      deepseek:  { type: openai, api-key: "${DEEPSEEK_API_KEY:}", base-url: https://api.deepseek.com/v1, model: deepseek-chat }
      local:     { type: ollama, base-url: http://localhost:11434, model: qwen2.5:7b }
    safety:
      approval-enabled: true
      sensitive-keywords: [删除, 转账, 下单, 退款]
```

- 模型新增/启停/改参需改配置并重启，无法在管理端列表操作；
- 敏感词增删需改配置并重启，无法动态维护；
- 无「能力类型」字段，无法区分文本/图片/视频生成能力；
- API Key 依赖环境变量注入，不同环境/租户无法独立配置。

### 1.2 目标

1. **模型配置库表动态化**：模型信息落库（含 API Key），管理端增删改、启停；运行态与前端下拉实时从库表读取；支持 `text / image / video / audio` 能力标记，并预留图片/视频生成落地路径。
2. **敏感词库表动态化**：敏感词落库，管理端增删改、启停；审批门禁实时按分级策略命中（提示/审批/拦截），替代 yaml 硬编码。
3. **平滑兼容**：库表为空或开关关闭时回退 yaml 配置，保证升级可启动（已确认决策）。

---

## 二、现状链路与关键发现

### 2.1 模型链路

| 环节 | 现状实现 |
| --- | --- |
| 配置源 | `AgentScopeProperties.models`（`Map<String, ModelProperties>`），字段：`type / apiKey / baseUrl / model / options`，apiKey 支持 `${ENV}` 占位 |
| 前端下拉 | `AgentManageService.models()` 遍历 `properties.getModels()` → `AgentModelVO(code/type/model)` |
| 运行解析 | `DefaultAgentTemplate.resolveModel()`：按 Agent 的 `model` 查 `properties.getModels()`，未命中用 `modelFactory.resolve(model)` |
| 实例创建 | `DefaultModelFactory.create(props)` 按 `type` 分发 DashScope / OpenAI / Ollama Provider |

### 2.2 敏感词链路

`DefaultAgentTemplate.approvalGate()` 现状：

```java
boolean keywordHit = safety.getSensitiveKeywords().stream()
        .anyMatch(k -> toolName.contains(k));   // 仅按工具名 contains 匹配
```

命中且 `approvalEnabled=true` 时，工具调用挂起走人工审批（`ApprovalRequest` → Redis → 审批中心）。

### 2.3 关键发现（v2 新增，直接影响设计）

1. **HarnessAgent 实例缓存**：`DefaultAgentTemplate.harnessCache`（`ConcurrentHashMap<String, HarnessAgent>`）按 Agent 名缓存 HarnessAgent，**模型在缓存构建时已绑定**。模型动态变更后若不清缓存，新配置对已构建的 Agent 无效。→ 必须引入「模型变更 → 失效对应/全部 Agent 缓存」机制。
2. **模型被引用校验缺失**：若某模型被启用的 `agent_definition.model` 或 `default-model` 依赖，直接停用会导致对话失败。→ 停用时需前置校验并给出兜底（回退默认模型）。
3. **底层能力边界**：`javap` 验证 AgentScope 2.0 `io.agentscope.core.model.Model` 仅有 `stream(List<Msg>, List<ToolSchema>, GenerateOptions)` 文本对话方法，**无原生图片/视频生成接口**。→ 图片/视频生成以「能力标记 + 生成类 `@AgentTool`」落地，预留生成 Provider 扩展点。

---

## 三、总体设计

### 3.1 架构分层

```
┌────────────────────────────── 管理端前端 ──────────────────────────────┐
│ 模型管理页(/agent/model)          敏感词库页(/agent/sensitive)          │
│ 增删改 · 启停 · 连接测试           增删改 · 启停 · 级别/匹配类型         │
└──────────────┬──────────────────────────┬─────────────────────────────┘
               │ REST                      │ REST
┌──────────────▼──────────────┐ ┌─────────▼────────────────────────────┐
│ zhush-module-ai             │ │ zhush-module-ai                      │
│ AiModelService              │ │ AiSensitiveWordService               │
│ · CRUD/启停/被引用校验       │ │ · CRUD/启停                          │
│ · listEnabled()/默认模型     │ │ · enabledWords()/分级缓存            │
│ · AES 加解密 api_key        │ │ · AC 自动机构建/命中检测              │
└──────────────┬──────────────┘ └─────────┬────────────────────────────┘
               │ ModelConfigProvider SPI   │ SensitiveWordProvider SPI
┌──────────────▼──────────────────────────▼────────────────────────────┐
│ zhush-spring-boot-starter-agentscope（不依赖业务模块）                 │
│ · AgentModelResolver：动态模型池(优先) → yaml(回退) → defaultModel     │
│ · SensitiveWordResolver：分级命中检测(优先) → yaml(回退)               │
│ · AgentTemplate.evictAgentCache()：模型变更失效 HarnessAgent 缓存      │
│ · 三级缓存刷新：本实例事件 → Redis pub/sub 广播 → 定时兜底              │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 动态化读取顺序（统一原则）

| 数据 | 优先级 1 | 优先级 2 | 优先级 3 |
| --- | --- | --- | --- |
| 模型配置 | `ai_model` 启用态（缓存） | `application.yaml` models | `default-model` 兜底 |
| 敏感词 | `ai_sensitive_word` 启用态（缓存） | `application.yaml` sensitive-keywords | — |

**回退规则**：`zhush.agentscope.dynamic-config.enabled=false` 或库表启用态数据为空 → 回退 yaml；两者皆空 → 启动告警日志（不阻断启动）。

---

## 四、模型动态化设计

### 4.1 数据表 `ai_model`

```sql
DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `code`          VARCHAR(64)  NOT NULL COMMENT '模型编码（唯一，Agent 定义 model 字段引用，如 qwen-max）',
    `name`          VARCHAR(128) NOT NULL COMMENT '展示名（如 通义千问-Max）',
    `type`          VARCHAR(32)  NOT NULL COMMENT '供应商标识：dashscope / openai / ollama',
    `api_key`       VARCHAR(1024) NOT NULL DEFAULT '' COMMENT 'API Key（AES 加密密文，支持 ${ENV} 占位）',
    `base_url`      VARCHAR(256) NOT NULL DEFAULT '' COMMENT '服务地址（openai 兼容服务）',
    `model`         VARCHAR(128) NOT NULL COMMENT '实际模型名（如 qwen-max、wanx2.1-t2i）',
    `capability`    VARCHAR(16)  NOT NULL DEFAULT 'text' COMMENT '能力类型：text-文本 image-图片 video-视频 audio-语音',
    `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认模型：0-否 1-是（全局唯一）',
    `priority`      INT          NOT NULL DEFAULT 0 COMMENT '优先级（下拉排序，越大越靠前）',
    `options`       JSON         NULL COMMENT '透传参数（JSON：temperature/top_p/size 等）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `is_delete`),
    KEY `idx_status_capability` (`status`, `capability`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 模型配置表';
```

> 变更：相较 v1，`api_key` 扩至 VARCHAR(1024) 承载加密密文；移除冗余 `version` 字段（配置更新语义由缓存刷新承担）。

### 4.2 后端分层（zhush-module-ai）

| 层 | 文件 | 说明 |
| --- | --- | --- |
| entity | `AiModelEntity` | 对齐表结构，继承 `BaseEntity` |
| mapper | `AiModelMapper` | `BaseMapperX`，`selectEnabled()`（status=1） |
| param | `AiModelParam` | 保存入参（apiKey 明文，服务层加密） |
| query | `AiModelQuery` | 分页筛选：code/type/capability/status |
| vo | `AiModelVO` | 列表视图，仅返回 `apiKeyMasked`（如 `sk-****`） |
| service | `AiModelService` | 见 §4.3 业务规则 |
| controller | `AiModelController` | `/admin/ai/model/*`（见 §七 接口清单） |
| provider | `DbModelConfigProvider` | 实现 starter 的 `ModelConfigProvider` SPI，向 starter 输出启用态模型 |

### 4.3 关键业务规则

1. **唯一性**：`code` 全局唯一（`uk_code` 约束 + 保存前查重）；`is_default=1` 全局唯一（设置新默认时事务内清除旧默认）。
2. **停用前置校验**：停用/删除前检查 `agent_definition`（status=1）中 `model` 字段引用：
   - 被引用 → 阻断操作，返回「该模型被 N 个启用智能体引用：xxx」；
   - 被引用且为全局默认模型 → 阻断并提示先切换默认模型。
3. **运行时兜底**：`resolveModel` 若动态池查无此模型（如已停用但 Agent 仍引用），回退默认模型并记录 WARN 日志，保证对话不中断。
4. **编辑语义**：前端提交 `apiKey` 为空或等于 `apiKeyMasked` → 保留原值；否则重新加密存储。
5. **连接测试**：新增 `POST /admin/ai/model/{id}/test`，用该模型发起一次最小文本对话（或按 capability 调对应探活接口），返回成功/失败与耗时。

### 4.4 API Key 安全设计

| 环节 | 策略 |
| --- | --- |
| 存储 | AES 对称加密（复用项目加密组件，密钥来自环境变量，与现有「API Key 脱敏安全策略」协同） |
| 读取 | 仅运行态解密用于构建 Model；管理端接口永远返回 `apiKeyMasked` |
| 日志 | 沿用 `AgentAuditEventListener` 脱敏规则（`sk-`/`api_key`/`secret`/`token` 正则替换 `***`） |
| 迁移 | `${ENV}` 占位仍支持：占位形式不加密、运行时解析环境变量（兼容存量） |

### 4.5 starter 侧改造

`AgentScopeProperties` 新增：

```yaml
zhush:
  agentscope:
    dynamic-config:
      enabled: true          # 动态配置总开关（false 回退 yaml）
      refresh-interval: 300s # 定时兜底刷新周期
      broadcast: true        # 多实例 Redis pub/sub 广播
```

- **新增 `ModelConfigProvider` 接口**（starter 定义，module-ai 实现）：`List<ModelConfig> enabledModels()`，`ModelConfig` 含 code/type/apiKey/baseUrl/model/capability/options。
- **新增 `AgentModelResolver`**：持有 provider + 本地缓存；`resolve(modelCode)` 输出 `ModelProperties`；缓存为空时回退 `properties.getModels()`。
- **`DefaultAgentTemplate.resolveModel()` 改造**：`agentRegistry` → `modelResolver.resolve()`（动态优先，回退逻辑见 §3.2）。
- **新增 `AgentTemplate.evictAgentCache(agentName)` / `evictAllAgentCache()`**：移除 `harnessCache` 对应条目（§2.3 发现 1 的修复点）。

---

## 五、敏感词库动态化设计

### 5.1 数据表 `ai_sensitive_word`

```sql
DROP TABLE IF EXISTS `ai_sensitive_word`;
CREATE TABLE `ai_sensitive_word` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `word`         VARCHAR(64)  NOT NULL COMMENT '敏感词',
    `match_type`   VARCHAR(16)  NOT NULL DEFAULT 'tool_name' COMMENT '匹配对象：tool_name-工具名 content-入参/内容 both-两者',
    `level`        TINYINT      NOT NULL DEFAULT 2 COMMENT '命中级别：1-提示 2-审批 3-拦截',
    `group_code`   VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '业务分组（默认 common）',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`, `is_delete`),
    KEY `idx_status_level` (`status`, `level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 敏感词库表';
```

### 5.2 分级命中策略（审批门禁增强）

`approvalGate()` 改造为三级决策，命中词取**最高级别**生效：

| 级别 | 行为 | 实现要点 |
| --- | --- | --- |
| 1-提示 | 放行执行，发 `AgentTextDeltaEvent` 提示文案 + 审计落库 | 不挂起，仅留痕 |
| 2-审批 | 现有审批链路（`ApprovalRequest` → Redis → 审批中心） | 保持兼容 |
| 3-拦截 | 直接返回拒绝 `ToolResultBlock`，不执行工具 | 拒绝理由写入事件供前端展示 |

命中逻辑按 `match_type` 分发：

- `tool_name` / `both`：对工具名匹配；
- `content` / `both`：对 `ToolCallParam.getInput()` 序列化后的 JSON 字符串匹配。

**匹配算法**：词库量级 ≤ 数千，采用 Aho-Corasick 自动机（或简单 `contains` 遍历，实现时二选一，接口层屏蔽差异）；词表变更时整树重建（低频，可接受）。

### 5.3 后端分层（zhush-module-ai）

| 层 | 文件 | 说明 |
| --- | --- | --- |
| entity | `AiSensitiveWordEntity` | 对齐表结构 |
| mapper | `AiSensitiveWordMapper` | `BaseMapperX`，`selectEnabled()` |
| param | `AiSensitiveWordParam` | 保存入参 |
| query | `AiSensitiveWordQuery` | 分页筛选：word/level/status/groupCode |
| vo | `AiSensitiveWordVO` | 列表视图 |
| service | `AiSensitiveWordService` | CRUD/启停 + `enabledWords()` 分级缓存 + 匹配结构重建 |
| controller | `AiSensitiveWordController` | `/admin/ai/sensitive/*` |
| provider | `DbSensitiveWordProvider` | 实现 starter 的 `SensitiveWordProvider` SPI |

starter 侧新增 `SensitiveWordResolver`：持有 provider + 缓存；`match(toolName, inputJson)` 返回命中结果（级别 + 命中词列表），空库回退 `properties.getSafety().getSensitiveKeywords()`（全部按级别 2 审批处理，保持现状语义）。

---

## 六、缓存刷新与多实例一致性

三级机制（自低到高）：

| 层级 | 触发 | 范围 | 说明 |
| --- | --- | --- | --- |
| L1 本实例 | Service 增删改/启停成功后 | 本实例 | 直接调 `modelResolver.refresh()` / `sensitiveWordResolver.refresh()` + `agentTemplate.evictAgentCache()` |
| L2 多实例广播 | 同上（`broadcast=true` 时） | 其他实例 | Redis pub/sub 频道 `agentscope:config:refresh`，订阅方本地刷新 + 失效 Agent 缓存 |
| L3 定时兜底 | 每 `refresh-interval`（默认 5 分钟） | 全部实例 | 防止广播丢失，低频全量重载 |

> 版本号优化（可选）：广播消息带 `updateTime`，订阅方比对本地版本号，无变化则跳过重载。

---

## 七、接口清单

### 7.1 模型管理 `/admin/ai/model`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | /page | `ai:model:list` | 分页（code/type/capability/status） |
| GET | /list | `agent:definition:list` | 启用态列表（Agent 表单下拉，含 capability） |
| GET | /{id} | `ai:model:list` | 详情（apiKeyMasked） |
| POST | / | `ai:model:create` | 新增（唯一性校验） |
| PUT | /{id} | `ai:model:update` | 编辑（apiKey 空则保留） |
| DELETE | /{id} | `ai:model:delete` | 删除（被引用阻断） |
| PUT | /{id}/toggle | `ai:model:update` | 启停（停用被引用阻断） |
| POST | /{id}/test | `ai:model:update` | 连接测试（最小对话探活） |

### 7.2 敏感词库 `/admin/ai/sensitive`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | /page | `ai:sensitive:list` | 分页（word/level/status/groupCode） |
| GET | /{id} | `ai:sensitive:list` | 详情 |
| POST | / | `ai:sensitive:create` | 新增（唯一性校验） |
| PUT | /{id} | `ai:sensitive:update` | 编辑 |
| DELETE | /{id} | `ai:sensitive:delete` | 删除 |
| PUT | /{id}/toggle | `ai:sensitive:update` | 启停 |

---

## 八、前端页面（vue-vben-admin / web-antdv-next）

### 8.1 模型管理 `views/agent/model/index.vue`

- 搜索区：编码/供应商/能力类型/状态；
- 列表列：序号、编码、名称、供应商（Tag）、能力类型（Tag：文本对话/图片生成/视频生成/语音）、实际模型名、默认（徽标）、优先级、状态（Switch 直切启停）、操作（编辑/测试/删除）；
- 编辑表单（`modules/form.vue`）：编码、名称、供应商、实际模型名、baseUrl、apiKey（编辑回填掩码）、能力类型、默认模型（二次确认互斥）、优先级、备注；
- 启停/删除前弹确认框，被引用时展示引用方。

### 8.2 敏感词库 `views/agent/sensitive/index.vue`

- 搜索区：敏感词/级别/状态/分组；
- 列表列：序号、敏感词、匹配对象（Tag）、级别（Tag：提示/审批/拦截）、分组、状态（Switch）、操作；
- 编辑表单：敏感词、匹配对象、级别、分组、备注；支持批量导入（textarea 换行分隔，后端批量校验去重）。

### 8.3 既有页面联动

- `views/agent/definition/modules/form.vue`：模型下拉改调 `getModelList()`（启用态），按 `capability` 分组展示（对话模型/生成模型）；
- `api/ai/types.ts`：新增 `AiModelVO / AiModelParam / AiSensitiveWordVO / AiSensitiveWordParam`；`AgentModelVO` 增 `capability / isDefault`。

均遵循既有列表规范：无标题、含序号列、文案统一、新增按钮在左、刷新在右。

---

## 九、SQL 脚本（`sql/15_agent_model_sensitive.sql`）

> 注意：`14_notice.sql` 已存在，本脚本编号顺延为 **15**。

内容组成（参照 `13_agent_ai.sql` 风格）：

1. `ai_model` 建表（§4.1）+ `ai_sensitive_word` 建表（§5.1）；
2. 初始化数据（对齐现有 yaml，迁移即可用）：

```sql
INSERT INTO `ai_model`(`id`, `code`, `name`, `type`, `api_key`, `base_url`, `model`, `capability`, `is_default`, `priority`, `status`) VALUES
  (93001, 'qwen-max',      '通义千问-Max',      'dashscope', '${DASHSCOPE_API_KEY:}', '',                              'qwen-max',      'text',  0, 10, 1),
  (93002, 'deepseek-chat', 'DeepSeek-Chat',    'openai',    '${DEEPSEEK_API_KEY:}',  'https://api.deepseek.com/v1', 'deepseek-chat', 'text',  1, 9,  1),
  (93003, 'local-qwen',    '本地 Ollama',       'ollama',    '',                      'http://localhost:11434',      'qwen2.5:7b',    'text',  0, 1,  0);

INSERT INTO `ai_sensitive_word`(`id`, `word`, `match_type`, `level`, `status`) VALUES
  (94001, '删除', 'tool_name', 2, 1),
  (94002, '转账', 'both',      3, 1),
  (94003, '下单', 'tool_name', 2, 1),
  (94004, '退款', 'tool_name', 2, 1);
```

3. 菜单权限（ID 约定延续 13 号脚本：按钮 = 菜单ID*10+序）：

```sql
-- 智能体管理目录(3010) 下新增
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3114, 3010, '模型管理', 2, 'ai:model:list', '/agent/model', '/agent/model/index', 5, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31141, 3114, '模型新增', 3, 'ai:model:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31142, 3114, '模型修改', 3, 'ai:model:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31143, 3114, '模型删除', 3, 'ai:model:delete', 3);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3115, 3010, '敏感词库', 2, 'ai:sensitive:list', '/agent/sensitive', '/agent/sensitive/index', 6, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31151, 3115, '敏感词新增', 3, 'ai:sensitive:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31152, 3115, '敏感词修改', 3, 'ai:sensitive:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31153, 3115, '敏感词删除', 3, 'ai:sensitive:delete', 3);

-- 授权超级管理员（显式落库，已确认决策）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES (99015, 1, 3114), (99016, 1, 31141), (99017, 1, 31142), (99018, 1, 31143),
       (99019, 1, 3115), (99020, 1, 31151), (99021, 1, 31152), (99022, 1, 31153);
```

---

## 十、图片/视频生成落地路径

### 10.1 本期（能力标记 + 生成类工具）

1. `ai_model.capability` 标记生成类模型（如 `wanx2.1-t2i` 文生图），前端按能力分组展示/筛选；
2. 生成调用走「生成类 `@AgentTool`」：业务工具内部按模型编码取配置（解密 apiKey），调供应商生成 API（如 DashScope 万相异步任务：提交 → 轮询 → 返回图片/视频 URL）；
3. 生成结果 URL 回填 `ToolResultBlock`，由 LLM 在回复中输出给用户。

### 10.2 扩展点（后续迭代）

- `GenerationAdapter` 接口（starter 定义）：`Mono<String> generateText(String prompt, ModelConfig cfg)`，按 `type` 实现各供应商适配器；
- 若 AgentScope 后续提供多模态 `Model` SPI，可在 `DefaultModelFactory` 中按 `capability` 分流到原生生成 Provider。

---

## 十一、涉及改动文件清单

### 后端

| 模块 | 类型 | 文件 |
| --- | --- | --- |
| sql | 新增 | `sql/15_agent_model_sensitive.sql` |
| starter | 新增 | `config/AgentScopeProperties.DynamicConfig`、`model/ModelConfigProvider`、`model/ModelConfig`、`model/AgentModelResolver`、`safety/SensitiveWordProvider`、`safety/SensitiveWordResolver` |
| starter | 修改 | `AgentScopeProperties`、`AgentScopeAutoConfiguration`（装配 resolver/订阅广播）、`DefaultAgentTemplate`（resolveModel/approvalGate/evictAgentCache）、`DefaultModelFactory`（capability 分流） |
| module-ai | 新增 | `AiModelEntity / AiModelMapper / AiModelParam / AiModelQuery / AiModelVO / AiModelService / AiModelController / DbModelConfigProvider` |
| module-ai | 新增 | `AiSensitiveWordEntity / AiSensitiveWordMapper / AiSensitiveWordParam / AiSensitiveWordQuery / AiSensitiveWordVO / AiSensitiveWordService / AiSensitiveWordController / DbSensitiveWordProvider` |
| module-ai | 修改 | `AgentManageService.models()`（改读动态模型池） |

### 前端（apps/web-antdv-next）

| 类型 | 文件 |
| --- | --- |
| 新增 | `api/ai/model.ts`、`api/ai/sensitive.ts` |
| 新增 | `views/agent/model/index.vue`、`views/agent/model/data.ts`、`views/agent/model/modules/form.vue` |
| 新增 | `views/agent/sensitive/index.vue`、`views/agent/sensitive/data.ts`、`views/agent/sensitive/modules/form.vue` |
| 修改 | `api/ai/types.ts`、`views/agent/definition/modules/form.vue`（模型下拉动态化） |

---

## 十二、风险与注意事项

| # | 风险 | 应对 |
| --- | --- | --- |
| 1 | 模型变更后旧 HarnessAgent 仍用旧模型 | `evictAgentCache()` 随刷新链路触发（L1/L2/L3 全覆盖） |
| 2 | 停用被引用的模型导致对话失败 | 停用时阻断 + 运行时查无模型回退默认模型（WARN 日志） |
| 3 | API Key 泄露 | AES 加密落库、接口仅返回掩码、日志脱敏沿用既有规则 |
| 4 | 多实例缓存不一致 | Redis pub/sub 广播 + 定时兜底双保险 |
| 5 | 升级环境无库表数据 | `dynamic-config.enabled=false` 或空库自动回退 yaml（已确认决策） |
| 6 | 敏感词误命中拦截正常工具 | 分级策略中「提示/审批」不阻断；拦截级别需管理端审慎配置 |
| 7 | 图片/视频生成无底层原生支持 | 本期走生成类工具 + capability 标记，预留 `GenerationAdapter` 扩展 |
| 8 | 菜单权限不生效 | 菜单显式写入 `sys_role_menu`（已确认决策） |

---

## 十三、验收标准

1. 模型管理列表可增删改、启停、连接测试；启停被引用模型时给出明确阻断提示；
2. 模型新增/变更后**无需重启**，新对话立即使用新模型（缓存刷新生效）；
3. `is_default` 互斥生效：设新默认后旧默认自动取消；
4. 敏感词库增删改/启停后立即按分级策略生效：提示不阻断、审批挂起、拦截拒绝；
5. `capability=image` 的模型在下拉与列表正确分组展示，可配置生成类工具；
6. 接口与页面永不返回完整 API Key（仅掩码）；
7. 库表为空 / `dynamic-config.enabled=false` 时系统以 yaml 配置正常启动（兼容回退）；
8. 双实例部署下，A 实例变更模型/敏感词后，B 实例在广播/兜底周期内同步生效。
