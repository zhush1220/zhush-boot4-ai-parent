# AI 模型配置与敏感词库动态化 —— 使用指南

> 面向使用者（管理员 / 运维 / 二次开发者）。实现细节见 [AI模型配置与敏感词库动态化实现说明.md](./AI模型配置与敏感词库动态化实现说明.md)。

## 1. 快速开始

### 1.1 部署步骤

1. **执行 SQL 脚本**（建表 + 初始数据 + 菜单权限）：

   ```bash
   mysql -u<user> -p <db> < sql/15_agent_model_sensitive.sql
   ```

2. **确认配置**（`zhush-server/src/main/resources/application-dev.yaml` 已内置，无需改动）：

   ```yaml
   zhush:
     agentscope:
       dynamic-config:
         enabled: true            # 动态库总开关
         refresh-interval: 300s   # 定时兜底刷新周期
         broadcast: true          # 多实例 Redis 广播
   ```

3. **重启服务**，使用超级管理员（`role_id=1`）登录管理端，即可在「AI 智能体」目录看到 **模型管理** 与 **敏感词库** 菜单。

> 若未执行 SQL 直接启动：系统不会报错，自动回退使用 yaml 中的 `zhush.agentscope.models` 与 `safety.sensitive-keywords` 静态配置。

### 1.2 权限分配

菜单权限已授权超级管理员角色。如需分配给其他角色，在「系统管理 → 角色管理」勾选：

- `ai:model:list / create / update / delete`
- `ai:sensitive:list / create / update / delete`

## 2. 模型管理操作指南

菜单路径：**AI 智能体 → 模型管理**（`/agent/model`）

### 2.1 新增模型

点击「新增模型」，填写：

| 字段 | 必填 | 说明 |
|---|---|---|
| 模型编码 | ✅ | Agent 定义引用的唯一标识（如 `qwen-plus`），**创建后不可修改** |
| 模型名称 | ✅ | 展示名称（如「通义千问-Plus」） |
| 供应商 | ✅ | DashScope（通义）/ OpenAI 兼容 / Ollama 本地 |
| 实际模型名 | ✅ | 如 `qwen-plus`、`gpt-4o`、`llama3` |
| API Key | 否 | 支持 `${ENV}` 环境变量占位（如 `${DASHSCOPE_API_KEY}`），真实 Key 加密落库 |
| 服务地址 | 条件 | OpenAI 兼容服务必填 |
| 能力类型 | 否 | 文本对话（默认）/ 图片生成 / 视频生成 / 语音合成 |
| 默认模型 | 否 | 开启后自动取消原默认模型 |
| 优先级 / 启用 / 备注 | 否 | — |

### 2.2 常用操作

- **启停**：列表 Switch 直接切换。停用后使用新配置创建会话即生效；**默认模型不可停用**（系统会阻止并提示）。
- **编辑**：模型编码锁定；API Key 留空表示保留原值，列表仅展示掩码（`sk-x****abcd`）。
- **连接测试**：点击「测试」发起真实模型调用（20 秒超时），验证 Key/服务地址是否可用。
- **删除**：被启用态 Agent 定义引用的模型禁止删除（提示「模型被 Agent 引用」）；需先在「Agent 管理」改绑其他模型。

### 2.3 图片 / 视频生成说明

`capability` 为**能力标记**：标记该模型具备图片/视频生成能力，供前端展示与后续生成类工具路由使用。Agent 对话链路（`Model.stream`）仍使用文本模型；图片/视频生成通过「能力标记 + 生成类 @AgentTool 扩展点」接入，参见设计方案文档。

### 2.4 与 Agent 定义的联动

在「Agent 管理」新增/编辑 Agent 时，模型下拉展示所有**启用态**模型（格式 `模型名称（编码）`）。在模型管理中启停模型后，下拉列表实时变化，无需刷新页面外的额外操作。

## 3. 敏感词库操作指南

菜单路径：**AI 智能体 → 敏感词库**（`/agent/sensitive`）

### 3.1 命中级别说明

| 级别 | 命中行为 | 适用场景 |
|---|---|---|
| 提示 | 记录 warn 日志后正常放行 | 低风险词、观察期词表 |
| 审批 | 工具调用挂起，进入「审批中心」待人工审批 | 转账、下单等高风险业务操作 |
| 拦截 | 工具调用直接拒绝，向模型返回错误信息 | 违规内容、绝对禁止的操作 |

### 3.2 匹配对象说明

| 值 | 匹配范围 |
|---|---|
| 工具名称（tool_name） | 仅匹配工具名 |
| 工具入参内容（content） | 仅匹配工具入参 JSON |
| 工具名与内容（both，默认） | 两者都匹配 |

### 3.3 使用示例

**场景 1**：所有含「转账」的工具调用必须人工审批 → 新增敏感词「转账」，级别选「审批」，匹配对象选「工具入参内容」。

**场景 2**：彻底禁止删除类操作 → 敏感词「删除」，级别「拦截」（初始数据已内置）。

**场景 3**：临时下线某条规则 → 列表中关闭该词的状态 Switch，立即生效；需要恢复时重新打开。

> 敏感词仅作用于 **Agent 工具调用门禁**，不影响普通聊天文本输出。

## 4. 接口清单

统一前缀、Sa-Token 登录鉴权 + 权限校验，返回 `Result` 包装。

### 4.1 模型管理 `/admin/ai/model`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/page` | `ai:model:list` | 分页（code/type/capability/status 模糊与精确查询） |
| GET | `/list` | `agent:definition:list` | 启用态列表（Agent 表单下拉用） |
| GET | `/{id}` | `ai:model:list` | 详情（apiKey 仅掩码） |
| POST | `/` | `ai:model:create` | 新增 |
| PUT | `/{id}` | `ai:model:update` | 编辑（apiKey 为空保留原值；code 忽略变更） |
| DELETE | `/{id}` | `ai:model:delete` | 删除（被引用时拒绝） |
| PUT | `/{id}/toggle` | `ai:model:update` | 启停切换 |
| POST | `/{id}/test` | `ai:model:update` | 连接测试 |

### 4.2 敏感词库 `/admin/ai/sensitive`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/page` | `ai:sensitive:list` | 分页（word/level/status/groupCode 查询） |
| GET | `/{id}` | `ai:sensitive:list` | 详情 |
| POST | `/` | `ai:sensitive:create` | 新增（词重复时报 1003302） |
| PUT | `/{id}` | `ai:sensitive:update` | 编辑 |
| DELETE | `/{id}` | `ai:sensitive:delete` | 删除 |
| PUT | `/{id}/toggle` | `ai:sensitive:update` | 启停切换 |

## 5. 配置参考

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `zhush.agentscope.dynamic-config.enabled` | `true` | `false` 完全回退 yaml 静态模式 |
| `zhush.agentscope.dynamic-config.refresh-interval` | `300s` | 定时兜底刷新周期 |
| `zhush.agentscope.dynamic-config.broadcast` | `true` | 多实例 Redis pub/sub 广播 |
| `zhush.agentscope.security.model-encrypt-key` | 内置默认 | API Key AES 加密密钥，**生产环境务必修改** |
| `zhush.agentscope.models` | — | 保留：动态库为空时的回退配置 |
| `zhush.agentscope.safety.sensitive-keywords` | — | 保留：词表为空时的回退配置 |
| `zhush.agentscope.safety.approval-enabled` | `true` | 审批级敏感词生效前提 |

## 6. 变更生效时效

| 部署形态 | 生效方式 |
|---|---|
| 单实例 | 保存即生效（本实例事件刷新 + Agent 缓存失效） |
| 多实例 + Redis 广播开启 | 保存即生效（pub/sub 频道 `agentscope:config:refresh` 广播） |
| 多实例 + 广播关闭 | 最迟 `refresh-interval`（默认 5 分钟）内生效 |

> 注意：进行中的会话已加载旧模型实例，模型变更在**下一次会话创建**时生效；敏感词变更对下一次工具调用立即生效。

## 7. 常见问题（FAQ）

**Q1：执行 SQL 前启动服务会怎样？**
A：正常启动，自动回退 yaml 静态配置，日志中可见 resolver 空载提示。执行 SQL 后无需重启，定时兜底刷新（最长 5 分钟）会自动加载库表数据；也可重启立即加载。

**Q2：API Key 填 `${DASHSCOPE_API_KEY}` 这种占位可以吗？**
A：可以。占位符原样存储（不加密），运行时从环境变量解析；解析失败回退原值。适合 K8s/Docker 环境注入密钥。

**Q3：把正在使用的模型停用/删除会怎样？**
A：停用：已运行会话不受影响，新建会话按「动态池 → yaml → 默认模型」链路兜底；删除：被引用时系统直接拒绝删除。

**Q4：敏感词命中拦截后用户看到什么？**
A：模型收到工具执行错误回执（`操作被拦截：检测到敏感内容[...]，请联系管理员`），通常会向用户解释原因；同时服务端记录审计日志。

**Q5：如何完全关闭动态库回到旧模式？**
A：设置 `zhush.agentscope.dynamic-config.enabled: false` 并重启，行为与旧版完全一致。
