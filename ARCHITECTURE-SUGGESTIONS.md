# zhush-boot4-parent 架构设计建议

> 基于项目当前状态（2026-07-09）的全面分析，本文档分为两部分：**现状问题诊断** + **架构设计建议**。

---

## 一、现状问题诊断

### 问题 1：根 POM 未聚合业务模块（严重）

当前根 POM 的 `<modules>` 中只声明了 `zhush-dependencies` 和 `zhush-framework`，未包含 `zhush-module-member` 和 `zhush-module-system`。这导致执行 `mvn clean install` 时，业务模块不会被统一构建，CI/CD 流程无法覆盖全部代码。

```xml
<!-- 当前状态（有问题）-->
<modules>
    <module>zhush-dependencies</module>
    <module>zhush-framework</module>
</modules>

<!-- 建议修改为 -->
<modules>
    <module>zhush-dependencies</module>
    <module>zhush-framework</module>
    <module>zhush-module-system</module>
    <module>zhush-module-member</module>
</modules>
```

### 问题 2：业务模块版本与主项目脱节（中等）

`zhush-module-member` 和 `zhush-module-system` 使用硬编码的 `0.0.1-SNAPSHOT` 版本，而主项目使用 `${revision}`（当前值为 `1.0.0`）。这破坏了版本统一性，且在发布时会导致依赖解析失败。

建议：将业务模块的 version 也改为 `${revision}`，并移除业务模块中 spring-boot-maven-plugin 对 spring-boot-starter-parent 的隐式依赖，改为在 dependencyManagement 中统一管理。

### 问题 3：starter-redis 的 parent 异常（中等）

`zhush-spring-boot-starter-redis` 的 parent 是 `spring-boot-starter-parent:4.1.0`，而所有其他 starter 的 parent 都是 `zhush-framework`。这破坏了继承链的一致性，也会导致依赖管理（如 Lombok、MapStruct-Plus 注解处理器）在该模块失效。

建议：将其 parent 统一改为 `zhush-framework`，并在其 `<dependencies>` 中显式引入 `spring-boot-starter-data-redis`（如需要）。

### 问题 4：Starter 模块缺少自动配置声明（严重）

Spring Boot 4 的自定义 Starter 需要在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中注册自动配置类（Spring Boot 3+ 已废弃 `spring.factories` 中的自动配置注册方式）。当前所有 starter 模块均未创建此文件，也没有任何 `@AutoConfiguration` 标注的类，这意味着这些 starter 即使被依赖，也不会生效。

建议：为每个 starter 模块创建对应的自动配置类，并注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中。

### 问题 5：配置文件极简，无环境隔离（中等）

所有 `application.yaml` 只定义了 `spring.application.name`，没有数据库连接、Redis 配置、端口号、日志级别等必要配置，也没有按环境（dev/test/prod）做配置分离。

建议：建立多环境配置体系（见后文"配置管理建议"章节）。

---

## 二、架构设计建议

### 2.1 目标架构分层

```
zhush-boot4-parent                          根 POM（聚合 + 构建管理）
│
├── zhush-dependencies                      BOM（第三方版本锁定）
│
├── zhush-framework                         框架层（聚合 POM）
│   ├── zhush-common                        基础层：POJO / 枚举 / 异常 / 工具类
│   │   ├── common-core                     [建议拆分] 核心基础类
│   │   ├── common-util                     [建议拆分] 工具类集合
│   │   └── common-domain                   [建议拆分] 领域基类（BaseEntity 等）
│   │
│   ├── starter-web                         Web 层：全局异常 / 响应封装 / 跨域 / 日志拦截
│   ├── starter-mybatis                     数据库：MyBatis-Flex 配置 / 多租户 / 分页 / 审计字段自动填充
│   ├── starter-redis                       缓存：Redisson 配置 / 缓存工具 / 分布式锁封装
│   ├── starter-satoken                     认证：Sa-Token 配置 / 登录拦截 / 权限注解扩展
│   ├── starter-protection                  防护：限流 / 幂等 / 防重复提交
│   ├── starter-mq                          消息：消息队列抽象（RocketMQ / RabbitMQ 切换）
│   ├── starter-job                         任务：定时任务框架集成（XXL-Job / PowerJob）
│   ├── starter-monitor                     监控：SkyWalking / Actuator / 链路追踪
│   ├── starter-excel                       Excel：导入导出封装（fesod-sheet / EasyExcel）
│   ├── starter-websocket                   WebSocket：消息推送封装
│   ├── starter-biz-ip                      IP 解析：ip2region 封装
│   ├── starter-biz-data-permission         数据权限：行级数据过滤拦截器
│   └── starter-file                        [建议新增] 文件存储：X-File-Storage 集成（本地/OSS/MinIO）
│
├── zhush-module-system                     系统管理模块（用户/角色/菜单/部门/字典/日志）
│   ├── system-biz                          [建议拆分] 业务实现层（Controller + Service + DAO）
│   └── system-api                          [建议拆分] 对外 API 接口（供其他模块依赖）
│
└── zhush-module-member                     会员管理模块
    ├── member-biz                          [建议拆分] 业务实现层
    └── member-api                          [建议拆分] 对外 API 接口
```

### 2.2 业务模块分层规范（每个业务模块内部）

每个业务模块（如 `zhush-module-system`）建议按以下包结构组织代码：

```
com.zhush.system/
├── controller/          接口层：HTTP 入口，参数校验，响应封装
│   └── admin/           后台管理接口（与前端 APP 接口分离）
│   └── app/             面向 C 端用户的接口
├── service/             业务逻辑层
│   └── impl/            接口实现
├── dal/                 数据访问层
│   ├── mapper/          MyBatis-Flex Mapper 接口
│   └── dataobject/      数据库实体对象（DO）
├── convert/             对象转换器（MapStruct-Plus 注解）
├── enums/               业务枚举
├── vo/                  视图对象（View Object，接口返回）
├── dto/                 数据传输对象（内部服务间传递）
└── config/              模块级配置类
```

### 2.3 zhush-common 模块建议补充的核心类

当前 `zhush-common` 为空壳，建议优先建设以下基础类：

```java
// === 统一响应封装 ===
public class CommonResult<T> {
    private Integer code;
    private String message;
    private T data;
    // 静态工厂方法：success() / error() / error(ErrorCode)
}

// === 分页请求与响应 ===
public class PageParam {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}

public class PageResult<T> {
    private List<T> list;
    private Long total;
}

// === 基础实体类 ===
public abstract class BaseEntity {
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creator;
    private String updater;
    private Boolean deleted;  // 逻辑删除
}

// === 全局异常体系 ===
public class ServiceException extends RuntimeException {
    private Integer code;
}

public interface ErrorCode {
    Integer getCode();
    String getMessage();
}

// === 常用枚举 ===
public enum CommonStatusEnum {
    ENABLE(0, "开启"),
    DISABLE(1, "关闭");
}
```

### 2.4 Starter 自动配置模板（以 starter-mybatis 为例）

每个 Starter 需要建立以下结构：

```
zhush-spring-boot-starter-mybatis/
├── src/main/java/com/zhush/mybatis/
│   ├── config/
│   │   └── ZhushMybatisAutoConfiguration.java    // @AutoConfiguration 标注
│   ├── core/
│   │   ├── mapper/BaseMapperX.java               // 扩展 BaseMapper
│   │   ├── handler/AuditMetaObjectHandler.java   // 审计字段自动填充
│   │   └── interceptor/DataPermissionInterceptor.java  // 数据权限拦截
│   └── ZhushMybatisProperties.java               // @ConfigurationProperties
├── src/main/resources/
│   └── META-INF/
│       └── spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── pom.xml
```

`AutoConfiguration.imports` 文件内容：
```
com.zhush.mybatis.config.ZhushMybatisAutoConfiguration
```

### 2.5 多数据库兼容策略

项目已引入 MySQL、PostgreSQL、达梦、人大金仓、Oracle 五套驱动，建议建立以下兼容机制：

1. **在 `starter-mybatis` 中建立 DatabaseDialect 抽象**：根据运行时激活的数据库类型自动切换方言（分页、日期函数、字符串拼接等差异点）。

2. **Mapper XML 中使用 `<if>` 条件判断**（如使用 XML 方式）：
   ```xml
   <if test="_databaseId == 'mysql'">
       LIMIT #{offset}, #{size}
   </if>
   <if test="_databaseId == 'oracle'">
       OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY
   </if>
   ```

3. **建议增加集成测试矩阵**：使用 Testcontainers 为每种数据库跑一遍核心流程。

### 2.6 配置管理建议（多环境）

建议在 `zhush-module-system`（主启动模块）中建立如下配置结构：

```
src/main/resources/
├── application.yaml              # 公共配置（日志格式、通用开关）
├── application-dev.yaml          # 开发环境
├── application-test.yaml         # 测试环境
├── application-prod.yaml         # 生产环境
└── logback-spring.xml            # 日志配置（按环境区分级别）
```

`application.yaml` 核心配置模板：
```yaml
spring:
  profiles:
    active: dev

server:
  port: 8080
  servlet:
    context-path: /api

# MyBatis-Flex
mybatis-flex:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# Sa-Token
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 86400
  is-concurrent: true

# Knife4j
knife4j:
  enable: true
  setting:
    language: zh_cn

# SkyWalking
skywalking:
  agent:
    service_name: zhush-system
```

### 2.7 安全设计建议

1. **接口认证链路**：`starter-satoken` 中配置全局登录拦截，通过 `SaInterceptor` 拦截所有请求，配合自定义 `@Permit` 注解标记白名单接口。

2. **数据权限**：`starter-biz-data-permission` 通过 MyBatis-Flex 的拦截器，在 SQL 执行前注入数据过滤条件（基于部门、角色、数据范围）。

3. **国密算法应用**：`zhush-common` 中已有 BouncyCastle 依赖，建议封装 `SM2Util`（接口签名验签）和 `SM4Util`（敏感字段加密存储，如手机号、身份证）。

4. **防重放攻击**：在 `starter-protection` 中实现基于 `nonce + timestamp` 的请求防重放机制，通过 Redis 记录已使用的 nonce。

### 2.8 构建与 CI/CD 建议

1. **Maven 构建命令**：
   ```bash
   # 全量构建（跳过测试）
   mvn clean install -DskipTests

   # 只构建框架层（快速迭代）
   mvn clean install -pl zhush-framework -am -DskipTests

   # 只构建某个业务模块
   mvn clean package -pl zhush-module-system -am -DskipTests
   ```

2. **建议增加 `.gitignore`**（如果还没有）：
   ```
   target/
   *.iml
   .idea/
   .vscode/
   *.log
   .flattened-pom.xml
   ```

3. **建议增加 `README.md`**：记录项目背景、技术栈、本地开发启动步骤、模块说明。

---

## 三、优先级建议（按顺序执行）

| 优先级 | 任务 | 原因 |
|--------|------|------|
| P0 | 修复根 POM，加入业务模块 | 否则统一构建失败 |
| P0 | 修复 starter-redis 的 parent 声明 | 继承链断裂，注解处理器失效 |
| P0 | 统一业务模块版本号（改为 `${revision}`） | 版本漂移会导致依赖解析失败 |
| P1 | 为 zhush-common 填充核心基础类 | 所有模块依赖它，空壳状态阻塞开发 |
| P1 | 为 starter-mybatis 建立自动配置类 | 数据库层是业务开发的前置条件 |
| P1 | 为 starter-web 建立自动配置类 | 统一响应/异常处理是接口开发前置条件 |
| P1 | 为 starter-satoken 建立自动配置类 | 认证是接口安全的前置条件 |
| P2 | 为 starter-redis 建立自动配置类 | 缓存和分布式锁 |
| P2 | 为 starter-protection 建立自动配置类 | 限流、幂等 |
| P2 | 建立多环境配置体系 | 开发/测试/生产隔离 |
| P3 | 完善其余 starter 自动配置 | job / mq / monitor / websocket 等 |
| P3 | 增加集成测试框架 | 保障代码质量 |

---

## 四、技术选型评估

| 技术 | 选型 | 评价 |
|------|------|------|
| Spring Boot 4.1.0 | Jakarta EE 体系 | 激进选型，生态兼容性需持续跟进（部分中间件尚未适配 Boot 4） |
| MyBatis-Flex 1.11.8 | ORM | 轻量灵活，社区活跃度低于 MyBatis-Plus，注意文档完整性 |
| Sa-Token 1.45.0 | 权限认证 | 国产轻量方案，上手快，企业级特性（SSO/OAuth2）需额外购买 |
| Redisson 4.6.1 | Redis 客户端 | 功能全面（分布式锁/队列/Bloom Filter），注意版本兼容性 |
| Knife4j 4.5.0 | API 文档 | OpenAPI 3 兼容，UI 体验好 |
| 国密算法（BouncyCastle） | 加密 | 满足等保/密评合规要求 |
| SkyWalking 9.6.0 | APM | 分布式链路追踪，与 Spring Boot 集成良好 |

**风险提示**：Spring Boot 4.x 于 2025 年底发布，目前处于生态早期阶段。部分第三方库（如某些版本的 MyBatis-Flex、Sa-Token、Knife4j）可能存在兼容性问题，建议在正式开发前做一次全量依赖树解析（`mvn dependency:tree`）和冒烟测试，确认无版本冲突。

---

*文档生成时间：2026-07-09*
*分析工具：QoderWork 自动化架构分析*
