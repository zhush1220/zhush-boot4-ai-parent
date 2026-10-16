# zhush-boot4-ai-parent

基于 **Spring Boot 4 / Java 21** 的多模块 AI 管理后台与智能体平台后端工程。系统以 [AgentScope](https://github.com/agentscope-ai) 智能体框架为核心，提供智能体对话（同步 + SSE 流式）、模型管理、Token 用量计量、配额与审批、敏感词合规、文档/图像/视频生成等 AI 能力，并内置一套完整的管理后台底座（用户、角色、菜单、部门、字典、定时任务、日志、监控、数据权限等）。

> 本仓库为纯后端工程，不含前端代码。

## 技术栈

| 领域 | 选型 | 版本 |
| --- | --- | --- |
| 语言 / 运行时 | Java | 21 |
| 应用框架 | Spring Boot / Spring Framework | 4.1.0 |
| ORM | MyBatis-Flex | 1.11.8 |
| 数据库连接池 | Druid | 1.2.28 |
| 数据库 | MySQL（默认），同时适配 PostgreSQL / 达梦 / 人大金仓 / Oracle | — |
| 缓存 | Redis（Redisson / spring-data-redis） | Redisson 4.6.1 |
| 认证鉴权 | Sa-Token（含 OAuth2 模块） | 1.46.0 |
| 智能体框架 | AgentScope（core / harness / 模型扩展 / redis / Jev） | 2.0.4 |
| 模型 SDK | 阿里云 DashScope SDK（qwen 系列、图像/视频生成） | 2.22.9 |
| 结构化决策 | Typesafe AI Jev（意图路由，可选） | — |
| 文档解析 | Apache Tika | 3.3.1 |
| 文档生成 | Apache POI（Word）、OpenPDF（PDF，内置 CJK 字体） | 5.5.1 / 2.0.3 |
| 消息队列 | RocketMQ（可选，默认关闭） | 5.5.0 |
| 全文搜索 | Elasticsearch（可选，默认关闭） | — |
| 定时任务 | Quartz（JDBC 持久化） | — |
| 接口文档 | Knife4j + SpringDoc OpenAPI | 5.2.4 / 3.0.3 |
| 监控 | SkyWalking、Spring Boot Admin（可选） | 9.6.0 / 4.1.2 |
| 微信生态 | WxJava（公众号 / 小程序 / 企业微信 / 开放平台 / 支付） | 4.8.0 |
| 行为验证码 | tianai-captcha | 1.5.5 |
| 配置加密 | Jasypt | 4.0.4 |
| 对象映射 | MapStruct Plus + Lombok | 1.5.1 / 1.18.46 |

## 工程结构

```
zhush-boot4-parent
├── zhush-dependencies          # BOM：全局依赖版本统一管理
├── zhush-framework             # 框架层聚合
│   ├── zhush-common            # 公共工具、通用模型、基础抽象
│   └── zhush-spring-boot-starter-*   # 18 个自研 Starter（见下表）
├── zhush-module-system         # 系统管理模块（管理后台底座）
├── zhush-module-member         # 会员模块（C 端用户、微信登录）
├── zhush-module-pay            # 统一支付底座（微信支付 V2/V3）
├── zhush-module-ai             # AI 智能体模块（核心业务）
└── zhush-server                # 唯一可执行启动模块，聚合各业务模块打包
```

依赖方向：`zhush-server → 业务模块 → framework starters → zhush-common`；版本由 `zhush-dependencies` 统一收敛，模块内依赖不写版本号。版本号通过 `flatten-maven-plugin` 的 `${revision}` 占位符统一管理（当前 `1.0.0`）。

### 自研 Starter 一览（zhush-framework）

| Starter | 职责 |
| --- | --- |
| `web` | Web 层封装：全局异常、统一返回体、跨域、应用路径信息 |
| `mybatis` | MyBatis-Flex 集成、多数据库方言适配、SQL 审计 |
| `redis` | Redis / Redisson 封装、缓存与分布式能力基座 |
| `satoken` | Sa-Token 认证集成、拦截器规则、管理端/会员端双域鉴权 |
| `oauth2-server` | OAuth2 授权服务端能力 |
| `biz-data-permission` | 数据权限：行级 SQL 过滤（`@DataPermission`）+ 字段级脱敏（`@FieldPermission`） |
| `biz-ip` | IP 归属地（ip2region） |
| `captcha` | 行为验证码（滑块/旋转/拼图） |
| `excel` | Excel 导入导出 |
| `file` | 多存储文件门面：本地 / 阿里云 OSS / 七牛 / 华为 OBS，切换只改配置 |
| `job` | Quartz 定时任务封装（JDBC 持久化，支持集群） |
| `monitor` | SkyWalking 与 Spring Boot Admin 监控 |
| `mq` | RocketMQ 消息队列封装 |
| `protection` | 接口幂等（`@Idempotent` + Redis 去重）等防护能力 |
| `search` | Elasticsearch 全文搜索 |
| `websocket` | WebSocket 推送 |
| `agentscope` | AgentScope 智能体框架集成：模型注册、会话存储、Jev 判断模型、事件异步分发 |
| `test` | 测试基础设施（JUnit5 + Mockito + AssertJ） |

## 核心功能

### AI 智能体模块（zhush-module-ai）

系统的核心业务模块，围绕 AgentScope 构建：

- **智能体对话**：`/admin/ai/agent` 提供同步对话与 SSE 流式输出，支持多会话管理、会话归属校验；模型回复支持停止、跟进与意图识别。
- **智能体管理**：智能体定义（`AgentDefinitionEntity`）持久化到库，支持工具（`AgentToolEntity`）注册与调用、执行日志（`AgentLogService`）。
- **模型管理**：`AiModelEntity` 维护多供应商模型配置（DashScope / OpenAI 兼容 / Ollama 本地模型），API Key 加密存储（`AiModelCipher`），支持数据库动态配置 + 定时/广播刷新，空库自动回退 YAML。
- **Token 用量与配额**：真实计量输入/输出 Token 并按日聚合（`AgentTokenUsageEntity`），支持成本估算（模型单价配置）与配额规则、限额校验（`AiQuotaRuleService` / `AiRateLimiter`）。
- **合规与安全**：敏感词库管理、合规守卫（`ComplianceGuard`）、危险操作人工审批流（`AgentApprovalService`，命中敏感词如"删除/转账/下单/退款"需人工放行）。
- **多模态生成**：基于 DashScope SDK 的图像/视频生成任务（异步 `asyncCall` + `fetch` 轮询，`AiGenerationTaskEntity` 记录任务）；文档产物生成（POI 输出 Word、OpenPDF 输出 PDF）；Tika 解析上传文档用于对话上下文。
- **Jev 结构化决策（可选）**：接入 Typesafe AI Jev 做意图路由与提示词润色，失败自动降级为普通对话（fail-open），不阻断主流程。
- **审计**：对话/工具执行审计日志（`AgentAuditLogEntity`）、Jev 决策记录采样落库。

### 系统管理模块（zhush-module-system）

管理后台底座，覆盖：

- 管理员用户、角色、菜单、部门、岗位、字典（类型/数据）、通知公告
- OAuth2 客户端管理、社交登录用户
- 登录日志、操作日志（支持 Excel 导出）、OpenAPI 调用日志、SQL 审计日志
- 定时任务管理与任务执行日志（Quartz）
- 文件管理（对接 `file` starter 多存储）
- 系统监控：Druid 统计、服务状态、Redis 缓存监控（授权接口输出，不裸露 Druid 页面）
- 数据权限资源管理：数据资源/字段的页面化配置（行级 + 字段级）
- 开放接口（`/open/**`）与微信企业号/开放平台集成

### 会员模块（zhush-module-member）

C 端会员体系：会员登录（账号 + 小程序 code 登录）、会员用户管理（管理端），接入 WxJava 公众号/小程序能力。

### 统一支付底座（zhush-module-pay）

协议无关的支付抽象层（core）+ 微信支付渠道适配（wx）：

- 微信 V3：商家转账 transfer-bills（有回调、可撤销）
- 微信 V2：企业付款到银行卡 ent-pay（无回调，定时查单收敛）
- 多商户路由预留、单笔出款限额（分维度配置）
- 全部凭证经环境变量外置，未启用不装配任何 Bean，启用缺凭证则启动失败（fail-fast）

## 认证与安全设计

- **双域鉴权**：`/api/admin/**` 走管理端登录态 + 接口级权限注解（`@SaCheckPermission(type="admin")`）；`/api/member/**` 走会员端登录态；白名单（登录、验证码、OAuth2、开放接口、回调、文档等）配置于 `zhush.security.ignore-paths`。
- **Token 策略**：`Authorization: Bearer` 请求头承载，不读写 Cookie（防 CSRF）；access-token 有效期 2 小时，refresh-token 无感续期；支持同账号多端并发登录、同端互踢。
- **其他防护**：接口幂等、行为验证码、SM4 参数加解密（`zhush.encrypt`）、SQL 审计、敏感词审批、数据权限（行级过滤 + 字段置空/脱敏）、CORS 可配置收敛。

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8+（库名 `zhush`，或改用已适配的 PG / 达梦 / 金仓 / Oracle）
- Redis 6+

### 构建

```bash
mvn clean install -DskipTests
```

### 运行

```bash
mvn -pl zhush-server spring-boot:run
# 或
java -jar zhush-server/target/zhush-server.jar --spring.profiles.active=dev
```

启动成功后（默认端口 `9166`，上下文 `/api`）：

| 入口 | 地址 |
| --- | --- |
| 接口文档（Knife4j） | http://localhost:9166/api/doc.html |
| Swagger UI | http://localhost:9166/api/swagger-ui.html |
| Druid 统计 | 由授权接口 `/admin/system/monitor/druid/**` 输出（Web 页面默认关闭） |
| Spring Boot Admin | `/api/monitor`（需 `zhush.monitor.enabled=true`） |

### 关键配置（`zhush-server/src/main/resources/application-dev.yaml`）

- 数据源：`mybatis-flex.datasource.master`（Druid 连接池、慢 SQL 日志）
- Redis：`spring.data.redis`
- AI 能力：`zhush.agentscope.*`（模型列表、会话存储 redis/memory 与 TTL、超时、事件异步分发、用量计量、动态配置、敏感词审批）
  - 模型 API Key 一律经环境变量注入：`DASHSCOPE_API_KEY`、`DEEPSEEK_API_KEY`；本地可用 Ollama 无需 Key
  - Jev 可选：`JEV_ENABLED=true` + `TYPESAFE_API_KEY`
- 可选开关：`zhush.search.enabled`（ES）、`zhush.mq.enabled`（RocketMQ）、`zhush.websocket.enabled`、`zhush.monitor.enabled`、`zhush.data-permission.enabled`、`zhush.job.enabled`
- 支付：`zhush.pay.wx.*`，凭证全部走 `WX_PAY_*` 环境变量
- 文件存储：`zhush.file.primary`（local / aliyun-oss / qiniu / huawei-obs）

> 生产环境请复制 dev 配置独立维护，敏感项使用环境变量或 Jasypt 加密注入，切勿提交真实密钥。

### 数据库脚本

`sql/` 目录存放增量脚本（如数据权限配置 `31/32_*.sql`、智能体清理 `30_*.sql`），按编号顺序执行；配置中引用的 Quartz 建表脚本（`sql/07_quartz.sql`）为启用 `zhush.job` 的前提，若仓库中缺失需按 MyBatis/Quartz 官方脚本自行初始化。

## 开发约定

- **包结构**（业务模块内统一分层）：`controller`（`admin` / `app` / `open` 分端）、`service`、`mapper`、`entity`、`param`（入参）、`query`（查询对象）、`vo`（出参）、`enums`。
- **命名**：管理端控制器 `Admin*Controller`，路径 `/admin/{module}/**`；实体 `*Entity`；权限注解使用 Sa-Token 双 type 区分管理端/会员端。
- **条件装配**：所有可选能力（支付、搜索、MQ、监控、数据权限、验证码等）默认关闭，通过 `zhush.*.enabled` 开关控制，关闭时零 Bean 装配、注解无副作用。
- **版本纪律**：依赖版本只允许出现在 `zhush-dependencies` BOM；已知冲突（victools jsonschema、commons-io、POI 与 Tika 对齐等）在 BOM 内以排除 + pin 方式解决，注释说明了原因，升级前先阅读。

## 文档索引

`docs/` 目录：

- 数据权限-行级业务表接入 checklist
- 数据权限-列级业务表接入 checklist
- 数据权限页面化配置解决方案 / 实施总结
- 数据权限框架审计评估报告

## Maven 仓库镜像

根 pom 已配置华为云、阿里云镜像加速依赖下载，并配置人大金仓官方仓库用于国产数据库驱动。
