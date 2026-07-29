# OAuth2.0 实现方案设计文档

> 适用项目：`zhush-boot4-parent`（Spring Boot 4.1.0 / Java 21 / Sa-Token 1.45.0 / MyBatis-Flex / Redis / MySQL 8.0+）
>
> 本文为**方案设计文档**，仅做分析与设计，不包含代码实现。落地时以本文为蓝本分阶段实施。
>
> **落地状态**：阶段一（角色 B 授权服务器 + 角色 C 开放 API）已实现并编译通过，见 §8 阶段一各条目标注；阶段二（角色 A 三方登录）未开始。
> **实现修正**：sa-token-oauth2 1.45.0 实际未内置 PKCE（`CodeModel` 无 `code_challenge` 字段），授权码模式按标准机密客户端实现，公开客户端（移动端/SPA）直连接入需待升级版本或自行扩展，现阶段建议经自有服务端中转；另 sa-token-oauth2 默认按明文比对 client_secret，为落地 BCrypt 存储策略，新增 `BCryptSecretOAuth2Template` 覆写密钥比对。
>
> 关联文档：`docs/登录与权限设计.md`

---

## 1. 现状分析

### 1.1 项目已有的认证基础

| 现状 | 说明 |
| --- | --- |
| 认证框架 | Sa-Token 多账号体系（`StpAdminUtil` / `StpMemberUtil`），**未引入 Spring Security** |
| Token 存储 | `sa-token-redis-template`，登录态集中存 Redis |
| 拦截器 | `SaTokenConfigure`（SaInterceptor），按 `/admin/**`、`/member/**` 前缀分端校验，白名单可经 `zhush.security.ignore-paths` 扩展 |
| 第三方绑定表 | 管理端 `sys_social_user`、会员端 `member_social_user` 已建表**预留** |
| 第三方登录入口 | `AdminAuthService.socialLogin` 为预留扩展点（当前抛 `SYSTEM_CONFIG_MISSING`）；会员端已实现微信小程序 `code2Session` 登录（非标准 OAuth2，属私有协议） |
| 权限模型 | 管理端 RBAC（用户→角色→菜单），`super_admin` 通配 `["*"]` |

### 1.2 OAuth2.0 在本项目中的三种角色

OAuth2.0 涉及三个不同的场景，需分开设计：

```
角色 A：作为 OAuth2 客户端（Client）
  本系统 --跳转--> 第三方授权服务器（微信开放平台/企业微信/钉钉/Gitee...）
  用途：让用户"使用第三方账号登录本系统"
  现状：表结构已预留（sys_social_user / member_social_user），逻辑未实现

角色 B：作为 OAuth2 授权服务器（Authorization Server）
  第三方应用 --跳转--> 本系统授权页 --> 本系统签发 access_token
  用途：开放平台能力，为第三方应用签发/刷新/撤销令牌
  现状：完全未实现

角色 C：作为 OAuth2 资源服务器（Resource Server，开放 API）
  第三方应用 --携带 access_token--> 本系统 /open/** 开放接口
  用途：第三方凭令牌调用本系统业务 API（本次明确的核心需求）
  现状：完全未实现，且现有 SaInterceptor 仅覆盖 /admin/**、/member/** 两端
```

**实施优先级：角色 B + 角色 C 成对落地（第三方调用接口依赖令牌签发），角色 A（三方登录）可并行或延后。**

---

## 2. 技术选型

### 2.1 角色 B（授权服务器）选型对比

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| **sa-token-oauth2**（推荐） | 与现有 Sa-Token 1.45.0 同版本同生态；复用现有 Redis 存储与多账号体系；轻量，仅需实现 `SaOAuth2DataLoader` 等少量接口；支持授权码、隐藏式、密码式、客户端凭证及 RefreshToken（注：1.45.0 未内置 PKCE，见文首实现修正） | 社区生态小于 Spring 官方；标准细节（如 OIDC、PKCE）支持不如专业实现 | ✅ **选用** |
| Spring Authorization Server | OAuth2.1/OIDC 标准最完整 | 强依赖 Spring Security，与现有 Sa-Token 拦截器体系冲突，需双安全框架并存，改造成本与心智负担大 | ❌ 排除 |
| 自研协议端点 | 完全可控 | 重复造轮子，安全细节（PKCE、防重放、token 撤销）易出漏洞 | ❌ 排除 |

### 2.2 角色 A（三方登录客户端）选型对比

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| **JustAuth**（推荐） | 一套 API 适配微信/企业微信/钉钉/Gitee/GitHub 等 70+ 平台；仅是工具库，无框架侵入；与现有 hutool 风格一致 | 个别平台更新需等版本 | ✅ **选用** |
| 各平台官方 SDK | 功能最全 | 每个平台一套 SDK，依赖膨胀、接口不统一 | ❌ 排除 |
| 手写 HTTP 调用（现有 `WxMiniClient` 模式） | 零新增依赖 | 每个渠道重复写授权码换 token、拿用户信息的样板代码 | 仅作兜底（JustAuth 不支持的私有协议，如小程序 code2Session，维持现状） |

> 依赖版本统一在 `zhush-dependencies/pom.xml` 中管理（`sa-token-oauth2` 复用 `${sa.token.version}`；`justauth` 新增版本属性），遵循项目"依赖版本全部收口到 BOM"的既有约定。

---

## 3. 总体架构

```
┌────────────────────────────────────────────────────────────────────┐
│                            浏览器 / APP / 第三方应用                  │
└──────┬──────────────────────┬──────────────────────────┬───────────┘
       │ 角色A：三方登录        │ 普通登录          │ 角色B：签发令牌      │ 角色C：凭令牌调用 API
       ▼                      ▼                  ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
│ /admin/auth/     │  │ /admin/auth/login│  │ /oauth2/authorize │  │ /open/v1/**          │
│   social-auth-url│  │ /member/auth/... │  │ /oauth2/token     │  │ （开放 API，携带      │
│ /admin/auth/     │  └─────────────────┘  │ /oauth2/refresh   │  │  Authorization:      │
│   social-login   │                        │ /oauth2/revoke    │  │  Bearer access_token）│
└──────┬──────────┘                        │ /oauth2/userinfo  │  └──────────┬──────────┘
       │                                    └──────────┬────────┘             │
       ▼                                               ▼                      ▼
┌──────────────────────────┐          ┌──────────────────────────────────────────────┐
│ zhush-spring-boot-       │          │ zhush-spring-boot-starter-oauth2-server       │
│ starter-biz-social       │          │ （sa-token-oauth2 封装，新增，含授权/资源两侧）  │
│ （JustAuth 封装，新增）    │          │ - OAuth2ServerAutoConfiguration               │
│ - SocialClientFactory    │          │ - SaOAuth2DataLoader 实现桥接                  │
│ - SocialUserInfo 统一模型 │          │ - 授权端点路由注册                             │
└──────┬──────────────────┘          │ - OpenApiInterceptor（/open/** token+scope 校验）│
       │                              └──────────┬───────────────────────────────────┘
       ▼                                         ▼
┌────────────────────────────────────────────────────────────────────┐
│  zhush-module-system（管理端绑定/登录逻辑、OAuth2 客户端管理 CRUD）     │
│  zhush-module-member（会员端绑定/登录逻辑）                            │
│  各业务模块 controller/open 包（OpenXxxController，开放 API 实现）      │
└──────────────────────────────┬─────────────────────────────────────┘
                               ▼
   MySQL（绑定表 / oauth2 客户端表 / 开放 API 调用日志） + Redis（token、授权码，Sa-Token 统一管理）
```

### 3.1 模块划分与依赖方向

| 模块 | 定位 | 说明 |
| --- | --- | --- |
| `zhush-spring-boot-starter-biz-social`（新增） | 框架层，角色 A | 封装 JustAuth：渠道配置（`@ConfigurationProperties("zhush.social")`）、`AuthRequest` 工厂、统一的 `SocialUserInfo`（openId/unionId/nickname/avatar）返回模型 |
| `zhush-spring-boot-starter-oauth2-server`（新增） | 框架层，角色 B + C | 封装 sa-token-oauth2：自动配置、端点注册、`SaOAuth2DataLoader`（客户端信息从 DB 加载）、scope 定义；资源侧提供 `/open/**` 的 token + scope 校验拦截器与 `@OpenApiScope` 注解 |
| `zhush-module-system` | 业务层 | 管理端三方登录/绑定落库；OAuth2 客户端（`sys_oauth2_client`）管理接口；授权确认页接口；开放 API 调用日志查询 |
| `zhush-module-member` | 业务层 | 会员端三方登录/绑定落库（复用静默注册模式） |
| 各业务模块 `controller/open` 包 | 业务层，角色 C | 开放 API 专用 Controller（`OpenXxxController`），与 `/admin/**`、`/member/**` 接口物理隔离，独立 DTO 裁剪返回字段 |

依赖方向约束（沿用现有规则，避免循环依赖）：

```
module-system / module-member ──> starter-biz-social ──> (justauth, 不依赖其他 starter)
module-system                 ──> starter-oauth2-server ──> starter-satoken ──> starter-web
```

`starter-oauth2-server` 只依赖 satoken starter，客户端数据的读取通过 `SaOAuth2DataLoader` 接口由 `zhush-module-system` 提供实现（与 `AdminStpInterfaceImpl` 实现 `StpInterface` 的模式一致）。

---

## 4. 角色 A：三方登录（OAuth2 客户端）设计

### 4.1 授权模式

采用标准**授权码模式（Authorization Code）**。PC 扫码类渠道（企业微信、钉钉、微信开放平台网站应用）均为此模式。

### 4.2 完整流程（以管理端接入企业微信为例）

```
1. 前端请求  GET /admin/auth/social-auth-url?socialType=WECOM
   后端用 JustAuth 生成授权地址（携带随机 state，state 存 Redis，TTL 5 分钟）
2. 前端跳转授权页 → 用户扫码/确认 → 第三方回调前端路由，携带 code + state
3. 前端调用  POST /admin/auth/social-login  { socialType, code, state }
4. 后端处理：
   a. 校验 state（Redis 中存在且未使用 → 删除，防 CSRF/重放）
   b. JustAuth 用 code 换 access_token 并拉取用户信息 → 得到 openId/unionId
   c. 查 sys_social_user (social_type, open_id)：
      - 已绑定 → 取 user_id 走现有 doLogin（同端互踢），返回 Sa-Token token
      - 未绑定 → 返回"未绑定"业务码 + 一次性 bindTicket（Redis，TTL 10 分钟）
5. 未绑定时前端引导账号密码验证：
   POST /admin/auth/social-bind  { bindTicket, username, password }
   验证通过 → 写入 sys_social_user 绑定关系 → 登录成功
```

会员端流程相同，差异点：未绑定时**不走绑定验证而是静默注册**（复用 `wxMiniLogin` 的模式），表为 `member_social_user`。

### 4.3 关键设计点

| 设计点 | 方案 |
| --- | --- |
| state 防伪 | 服务端生成并存 Redis，回调时一次性核销；不信任前端传入的自定义 state |
| 管理端绑定策略 | 显式绑定（需密码验证），**不做静默注册**——管理员账号必须后台预先创建 |
| 会员端绑定策略 | 静默注册，`register_type` 标记渠道来源 |
| unionId 归一 | 微信系渠道（小程序/公众号/开放平台）优先按 `union_id` 归并到同一 `member_user`，避免一人多号 |
| 解绑 | 提供 `POST /admin/auth/social-unbind`；管理端解绑前校验该账号仍可密码登录（防失联） |
| 白名单 | `social-auth-url`、`social-login`、`social-bind` 加入 `SaTokenConfigure` 内置白名单或经 `zhush.security.ignore-paths` 配置 |
| 已实现的小程序登录 | 微信小程序 `code2Session` 属私有协议而非标准 OAuth2，**维持现有 `WxMiniClient` 实现不动**，仅在文档层面归入"社交登录"能力 |

### 4.4 配置示例（application.yml）

```yaml
zhush:
  social:
    channels:
      WECOM:                      # 企业微信扫码
        client-id: ww-corpid
        client-secret: xxx
        agent-id: 1000002
        redirect-uri: https://admin.example.com/social-callback/wecom
      DINGTALK:
        client-id: ding-appkey
        client-secret: xxx
        redirect-uri: https://admin.example.com/social-callback/dingtalk
      GITEE:
        client-id: xxx
        client-secret: xxx
        redirect-uri: https://admin.example.com/social-callback/gitee
```

### 4.5 表结构（已建，无需改动）

`sys_social_user`（`social_type` + `open_id` 唯一）与 `member_social_user`（含 `union_id`、`session_key`）现有结构可直接满足，仅需按需扩充 `social_type` 字典值（`WECOM` / `DINGTALK` / `WECHAT_OPEN` / `GITEE` ...）。

---

## 5. 角色 B：授权服务器（sa-token-oauth2）设计

### 5.1 支持的授权模式

| 模式 | grant_type | 是否开启 | 适用场景 |
| --- | --- | --- | --- |
| 授权码 | `authorization_code` | ✅ 默认开启 | 第三方 Web 应用、服务端接入（**主推**；1.45.0 无内置 PKCE，公开客户端需经自有服务端中转） |
| 刷新令牌 | `refresh_token` | ✅ 开启 | 续期 access_token |
| 客户端凭证 | `client_credentials` | ✅ 开启 | 服务端对服务端（无用户上下文的 API 调用） |
| 隐藏式 | `implicit` | ❌ 关闭 | OAuth2.1 已废弃，不开放 |
| 密码式 | `password` | ❌ 关闭 | OAuth2.1 已废弃，不开放（避免第三方触碰用户密码） |

### 5.2 端点设计

统一前缀 `/oauth2/**`（sa-token-oauth2 内置路由，需加入鉴权白名单，由 OAuth2 协议自身完成校验）：

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/oauth2/authorize` | GET | 授权入口：未登录 → 重定向到统一登录页；已登录且客户端非白名单 → 返回授权确认页数据（应用名、申请的 scope） |
| `/oauth2/doConfirm` | POST | 用户点击"同意授权"，签发授权码（授权码 TTL 5 分钟，一次性） |
| `/oauth2/token` | POST | code + client_id + client_secret 换 access_token / refresh_token |
| `/oauth2/refresh` | POST | refresh_token 换新 access_token |
| `/oauth2/revoke` | POST | 撤销 access_token |
| `/oauth2/userinfo` | GET | 资源端点：凭 access_token 返回用户基础信息（受 scope 控制） |

### 5.3 scope 规划

| scope | 含义 |
| --- | --- |
| `openid` | 仅返回用户唯一标识 |
| `userinfo` | 昵称、头像等基础资料 |
| `roles` | 用户角色列表（仅管理端账号体系有意义） |

scope 与返回字段的映射在 `starter-oauth2-server` 内集中定义，第三方申请超出其注册范围的 scope 直接拒绝。

### 5.4 客户端管理表（新增 DDL：`sql/05_oauth2.sql`）

`sys_oauth2_client`（沿用项目统一约定：雪花 `id`、`BaseEntity` 公共字段、`is_delete` 逻辑删除、utf8mb4/InnoDB）：

| 字段 | 说明 |
| --- | --- |
| `client_id` | 客户端标识，唯一索引 `uk_client_id(client_id, is_delete)` |
| `client_secret` | 客户端密钥（BCrypt 存储，与用户密码同策略） |
| `client_name` / `logo` | 应用名称与图标（授权确认页展示） |
| `grant_types` | 允许的授权类型，逗号分隔 |
| `scopes` | 允许申请的 scope，逗号分隔 |
| `redirect_uris` | 回调地址白名单，逗号分隔，**精确匹配** |
| `access_token_timeout` / `refresh_token_timeout` | 令牌有效期（秒），空则用全局默认 |
| `qps_limit` / `daily_quota` | 开放 API 限流 QPS / 日调用配额，空则用全局默认（见 §6.5） |
| `notify_url` | 回调通知地址（预留，见 §6.6） |
| `auto_confirm` | 是否免确认页（自有信任应用可设 1） |
| `status` | 启用/停用 |

管理接口：`/admin/oauth2/client/**` CRUD（走现有 RBAC 权限，如 `system:oauth2-client:add`），`client_secret` 创建时明文仅回显一次。

### 5.5 与现有 Sa-Token 体系的整合

- **登录态复用**：`/oauth2/authorize` 判定登录使用 `StpAdminUtil`（默认对管理端账号开放；若开放平台面向会员，另注册基于 `member` 账号体系的 OAuth2 实例，两套隔离）；
- **数据桥接**：`SaOAuth2DataLoader.getClientModel(clientId)` 由 `zhush-module-system` 实现，从 `sys_oauth2_client` 读取并做缓存（Redis，变更时失效）；
- **令牌存储**：授权码、access_token、refresh_token 全部由 sa-token-oauth2 写入现有 Redis，无新增存储组件；
- **拦截器**：`/oauth2/**` 加入 `SaTokenConfigure` 白名单（协议端点自行校验）；`/oauth2/userinfo` 与 `/open/**` 由 OAuth2 的 access_token 校验保护而非登录态 `SaInterceptor`（见 §6）。

### 5.6 令牌策略

| 项 | 默认值 | 说明 |
| --- | --- | --- |
| 授权码 TTL | 5 分钟 | 一次性，使用即销毁 |
| access_token TTL | 2 小时 | 短有效期，降低泄露风险 |
| refresh_token TTL | 30 天 | 与现有 `sa-token.timeout` 对齐 |
| token 撤销 | 支持 | 客户端停用/用户解除授权时批量撤销 |
| PKCE | 1.45.0 未内置，本期不启用 | 移动端/SPA 等公开客户端暂经自有服务端中转，后续升级版本或自行扩展 `code_challenge`(S256) |

---

## 6. 角色 C：开放 API（资源服务器）设计

第三方应用拿到 access_token 后，凭令牌调用本系统业务接口。本章是"接口可供第三方调用"需求的核心设计。

### 6.1 路由与接口隔离原则

新增独立端前缀 `/open/v1/**`，与现有 `/admin/**`、`/member/**` 平行，成为第三个"端"：

| 原则 | 说明 |
| --- | --- |
| 物理隔离 | 开放 API 一律新建 `OpenXxxController`（各业务模块 `controller/open` 包），**禁止直接把内部管理接口暴露给第三方**，避免内部接口变更破坏对外契约 |
| 独立 DTO | 开放 API 使用独立的 `OpenXxxVO`/`OpenXxxParam`，只暴露必要字段（不返回内部 id 关联、审计字段、敏感信息） |
| 版本化 | URL 携带版本号 `/open/v1/`，不兼容变更升 `/open/v2/`，旧版本给迁移期 |
| 契约优先 | 开放 API 单独分组输出 OpenAPI 文档（springdoc group `open`），供第三方对接 |
| 统一响应 | 复用现有 `CommonResult` 响应结构；协议层错误（token 无效/scope 不足）按 OAuth2 规范返回 `401/403` + `error`/`error_description` 字段 |

### 6.2 鉴权链路（OpenApiInterceptor）

`starter-oauth2-server` 提供开放 API 拦截器，注册到 `/open/**`，校验顺序：

```
1. 提取 Authorization: Bearer <access_token>（缺失 → 401 invalid_request）
2. sa-token-oauth2 校验 token 有效性（无效/过期/已撤销 → 401 invalid_token）
3. 加载 token 关联的 client_id → 校验客户端 status 启用中（停用 → 401）
4. scope 校验：接口声明的 @OpenApiScope("xxx") ⊆ token 持有 scope（不足 → 403 insufficient_scope）
5. 限流/配额校验（超限 → 429）
6. 写入调用上下文 OpenApiContext（clientId、scope、可选 loginId）→ 放行至 Controller
7. 后置：异步记录调用日志（sys_open_api_log）
```

与现有体系的关系：`SaTokenConfigure` 中 `/open/**` 不走 `StpAdminUtil`/`StpMemberUtil` 登录态校验（加入白名单），鉴权完全由 `OpenApiInterceptor` 接管，两套拦截器职责不交叉。

### 6.3 两类调用语义

| 调用类型 | 令牌来源 | 用户上下文 | 典型接口 |
| --- | --- | --- | --- |
| 用户级调用 | 授权码模式签发的 token | 有（token 关联 loginId，可按用户隔离数据） | “获取当前授权用户的订单列表” |
| 应用级调用 | client_credentials 模式签发的 token | 无（仅 clientId） | “拉取商品目录”“推送回调确认” |

接口设计时必须声明所需语义：仅支持用户级的接口在拦截器校验 token 必须携带用户上下文，否则返回 403。审计字段（`create_by`）在开放 API 写操作时取 `OpenApiContext` 的 loginId，应用级调用写入约定的系统账号标识。

### 6.4 scope 与开放能力的映射

在 §5.3 基础 scope 之上，按业务域扩展资源类 scope（命名规则 `域:操作`，与现有 RBAC 权限码风格对齐）：

| scope 示例 | 含义 |
| --- | --- |
| `member:read` | 读取授权会员的基础资料 |
| `order:read` / `order:write` | 订单查询 / 创建（示例，按实际业务域定义） |
| `open:notify` | 接收回调确认类应用级接口 |

约束：客户端注册时分配可申请 scope 集合（存 `sys_oauth2_client.scopes`）；接口侧用 `@OpenApiScope` 声明所需 scope，新增开放接口不需要改拦截器，只需加注解。

### 6.5 限流、配额与审计

| 机制 | 方案 |
| --- | --- |
| 限流 | 按 `client_id` 维度双重限制（`OpenApiRateLimiterImpl`，Redisson 固定窗口计数）：秒级 QPS + 自然日配额，阈值存 `sys_oauth2_client` 扩展字段（`qps_limit`、`daily_quota`，空则用 `zhush.oauth2.default-qps-limit`/`default-daily-quota` 全局默认，0 或负数不限制）；阈值本地缓存 60s，Redis 异常时放行（fail-open） |
| 超限响应 | `429 Too Many Requests`，携带 `Retry-After` |
| 调用日志 | 新增 `sys_open_api_log`（clientId、接口、耗时、状态码、请求 IP、traceId），异步写入，参照现有 `sys_operate_log` 的实现模式；管理端提供查询页 |
| 监控 | 按 clientId 统计调用量/错误率，接入现有 monitor 体系（SBA） |

### 6.6 回调通知（可选，预留）

若第三方需要接收本系统事件推送（如订单状态变更），在 `sys_oauth2_client` 预留 `notify_url` 字段；推送报文附 HMAC-SHA256 签名（以 client_secret 衍生密钥），第三方验签防伪造；重试 + 死信记录。本期仅留字段与接口形态，不实现。

---

## 7. 安全设计清单

| 风险 | 对策 |
| --- | --- |
| CSRF（三方登录回调伪造） | state 服务端生成 + Redis 一次性核销 |
| 授权码劫持 | 授权码一次性 + 5 分钟 TTL；PKCE 待后续版本支持，现阶段不向公开客户端直接开放授权码模式 |
| 回调地址篡改 | `redirect_uris` 白名单精确匹配，不支持通配 |
| client_secret 泄露 | BCrypt 落库、仅创建时回显一次；支持重置并自动撤销存量 token |
| scope 越权 | 申请 scope ⊆ 注册 scope，userinfo 按 scope 裁剪字段 |
| 密码模式风险 | 直接关闭 `password` / `implicit` 模式（对齐 OAuth2.1） |
| 令牌泄露扩散 | access_token 短 TTL + 可撤销；全链路强制 HTTPS |
| 账号探测 | 三方绑定验证失败与密码登录一致，统一模糊报错（沿用现有约定） |
| 暴力尝试 | `/oauth2/token`、`social-bind` 接入限流（可复用 `zhush-spring-boot-starter-protection`） |
| 开放 API 越权 | `/open/**` 独立拦截器校验 token + scope；接口物理隔离 + 独立 DTO，不复用内部管理接口 |
| 用户数据串访 | 用户级接口强制从 token 解析 loginId 做数据过滤，禁止从请求参数接收用户标识 |
| 单客户端滥用 | 按 client_id 限流 + 日配额 + 调用日志审计，可随时停用客户端并批量撤销其 token |

---

## 8. 实施路线图

### 阶段一：授权服务器 + 开放 API（角色 B + C，本次核心需求，建议先行）

1. ✅ `zhush-dependencies` 增加 `sa-token-oauth2`（复用 `${sa.token.version}`）与新 starter 版本管理；
2. ✅ 新建 `zhush-spring-boot-starter-oauth2-server`：自动配置（`OAuth2ServerAutoConfiguration`）、协议端点（`OAuth2ServerEndpoint` 统一分发 `/oauth2/*`）、scope 定义（`OAuth2Scopes`）、令牌策略配置项（`zhush.oauth2.*`）；资源侧 `OpenApiInterceptor`、`@OpenApiScope`、`OpenApiContext`；额外新增 `BCryptSecretOAuth2Template`（BCrypt 密钥比对）与三个自定义 SPI（`OAuth2DoLoginHandler`、`OpenApiLogRecorder`、`OpenApiRateLimiter`，另 `SaOAuth2DataLoader` 由业务模块实现）；
3. ✅ 新增 `sql/05_oauth2.sql`：`sys_oauth2_client`（含限流/配额/notify_url 字段）、`sys_open_api_log`；
4. ✅ `zhush-module-system`：`Oauth2DataLoaderImpl` 实现 `SaOAuth2DataLoader`（停用/删除返回 null，存量令牌立即失效）；客户端管理 CRUD + 密钥重置（`/admin/system/oauth2-client/**`，权限码 `system:oauth2-client:*`，密钥明文仅回显一次）；`OpenApiLogService` 异步落库 + 查询接口（`/admin/system/open-api-log/**`）；`Oauth2DoLoginHandlerImpl` 授权页登录；`OpenApiRateLimiterImpl` 限流器（Redisson 秒级 QPS + 日配额，见 §6.5）；
5. ✅ 首批开放 API 示例 `OpenUserController`（`GET /open/v1/user/me`，`@OpenApiScope(value="userinfo", requireUser=true)`）；springdoc 增加“开放平台”分组（`/oauth2/**`、`/open/**`）；
6. ✅ `SaTokenConfigure` 白名单增加 `/oauth2/**`、`/open/**`；⬜ 前端配套统一登录页与授权确认页（后端已以 JSON 形式返回 notLoginView/confirmView 数据，待前端实现）；
7. ⬜ 联调验证：授权码全流程（无 PKCE，见文首修正）、client_credentials、refresh、revoke、scope 裁剪、回调白名单拒绝、开放 API 鉴权与限流（需先执行 `sql/05_oauth2.sql` 并注册测试客户端）。

> 阶段一代码已全部落地，`mvn -pl zhush-server -am compile` BUILD SUCCESS（JDK 21）。限流 SPI（`OpenApiRateLimiter`）已由 `zhush-module-system` 的 `OpenApiRateLimiterImpl` 实现（Redisson 固定窗口：秒级 QPS + 自然日配额，全局默认值可通过 `zhush.oauth2.default-qps-limit`/`default-daily-quota` 调整）。

### 阶段二：三方登录（角色 A，可并行或延后）

1. `zhush-dependencies` 增加 JustAuth 版本管理；
2. 新建 `zhush-spring-boot-starter-biz-social`：配置属性、`AuthRequest` 工厂、`SocialUserInfo` 模型、state 管理；
3. `zhush-module-system`：实现 `socialLogin`（替换现有预留异常）、新增 `social-auth-url` / `social-bind` / `social-unbind` 接口，落库 `sys_social_user`；
4. `zhush-module-member`：新增会员端标准 OAuth2 渠道（公众号网页授权、开放平台）静默注册登录，落库 `member_social_user`，处理 unionId 归并；
5. 更新 `SaTokenConfigure` 白名单与 `docs/登录与权限设计.md` §12 扩展点状态。

### 验收要点

- 授权服务器：`password`/`implicit` 请求返回不支持；非白名单 redirect_uri 被拒；access_token 过期后 refresh 可续期、revoke 后立即失效；
- 开放 API：无 token/错 token 返回 401，scope 不足返回 403，超限返回 429；停用客户端后其存量 token 立即不可用；用户级接口仅返回授权用户自身数据；调用日志完整落库；
- 三方登录：state 重放请求被拒；未绑定管理员走绑定流程、未绑定会员静默注册；同端互踢行为与密码登录一致；
- 构建：`mvn -DskipTests install` BUILD SUCCESS（以日志为准，不看 PowerShell exit code）。

---

## 9. 决策摘要

| 决策 | 结论 |
| --- | --- |
| 授权服务器实现 | sa-token-oauth2（同生态复用，排除 Spring Authorization Server） |
| 三方登录实现 | JustAuth 统一封装为新 starter |
| 开放的授权模式 | 授权码、refresh_token、client_credentials；关闭 password/implicit；PKCE 待版本升级后启用 |
| 第三方调用接口 | 新增 `/open/v1/**` 独立端，`OpenApiInterceptor` 校验 Bearer token + scope，接口与内部端物理隔离 |
| 开放 API 治理 | 按 client_id 限流/日配额，`sys_open_api_log` 异步审计，springdoc 独立 `open` 文档分组 |
| 存储 | 令牌/授权码/state 全走现有 Redis；客户端、绑定关系、调用日志走 MySQL |
| 表结构 | 复用 `sys_social_user`、`member_social_user`；新增 `sys_oauth2_client`、`sys_open_api_log` |
| 绑定策略 | 管理端显式绑定（密码验证），会员端静默注册 + unionId 归并 |
| 实施顺序 | 先授权服务器+开放 API（阶段一，支撑第三方调用），后三方登录（阶段二） |
