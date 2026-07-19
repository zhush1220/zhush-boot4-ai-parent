# zhush-boot4-parent 架构分析报告（v3）

> 分析日期：2026-07-11（第二次重新分析）
> 项目版本：1.0.0（${revision}）
> 技术栈：Spring Boot 4.1.0 / Java 21 / MyBatis-Flex 1.11.8 / Sa-Token 1.45.0 / Redisson 4.6.1

---

## 一、v2 问题修复追踪

对比上一轮（v2）报告的 10 个问题，修复情况如下：

| # | v2 问题 | 状态 | 说明 |
|---|---------|------|------|
| 1 | AutoConfiguration.imports 为空 | ✅ 已修复 | mybatis starter 已注册 `GlobalFlexAutoConfiguration` |
| 2 | spring-boot-maven-plugin 缺版本号 | ✅ 已修复 | 已添加 `<version>${spring.boot.version}</version>` |
| 3 | 业务模块零依赖框架 Starter | ✅ 已修复 | module-system/member 均已引入 web/mybatis/redis/satoken |
| 4 | Jackson 2.x/3.x 版本混用 | ⚪ 误报撤回 | `com.fasterxml.jackson.annotation` 是 Jackson 3.x 注解包的正确命名空间，非版本混用 |
| 5 | starter-web 无任何依赖 | ✅ 已修复 | 已添加 zhush-common + spring-boot-starter-web |
| 6 | Listener userId 硬编码 | ❌ 未修复 | 仍返回 `1L` |
| 7 | 缺少 spring.profiles.active | ✅ 已修复 | 已设置 `active: dev` |
| 8 | 多环境配置无实质内容 | ⚠️ 部分修复 | 已添加端口/MyBatis-Flex/日志配置，但仍缺数据源/Redis/Sa-Token |
| 9 | zhush-common Lombok scope 不一致 | ✅ 已修复 | 已改为 provided |
| 10 | 测试类缺启动类 | ❌ 未修复 | module-system/member 测试仍无法运行 |

**v2 修复率：6/10 已修复，1 个误报撤回，2 个未修复，1 个部分修复。**

此外还观察到以下**积极变化**：

- 新增 `BaseMapperX`（分页/排序/批量操作增强）和 `SortablePageQuery`/`SortingField`（排序分页支持）
- starter-mybatis 代码结构优化：listener 移至 `core.listener` 包下，新增 `core.mapper` 和 `core.query` 包
- starter-mybatis 显式声明了 zhush-common 直接依赖（之前仅通过 satoken 间接依赖）
- 根 POM 新增 `spring.boot.version` 属性和华为云/阿里云 Maven 镜像
- zhush-server 添加了 `<finalName>` 控制打包名称
- 新增 `application-my.yaml` 个人开发环境配置

---

## 二、当前问题诊断

### 问题 1：Listener 中 userId 硬编码为 1L（严重 🔴）

`GlobalInsertListener.getUserId()` 和 `GlobalUpdateListener.getUserId()` 均硬编码返回 `1L`。所有继承 `BaseEntity` 的实体在插入/更新时，`createBy` 和 `updateBy` 字段都会被错误地填充为固定值 1，无法追踪实际操作人。

**修复方案**：通过 Sa-Token 获取当前登录用户，同时兼容无用户上下文的场景：

```java
private Long getUserId() {
    try {
        // 通过 Sa-Token 获取当前登录用户ID
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    } catch (Exception e) {
        // 系统级操作（定时任务、数据迁移等）无用户上下文
        return 0L;
    }
}
```

由于 starter-mybatis 已将 starter-satoken 声明为 `provided` scope，Sa-Token 类在编译期可用、运行期由消费方模块提供，上述代码可以正常工作。但建议在 AutoConfiguration 中增加 `@ConditionalOnClass` 判断，当项目不引入 Sa-Token 时回退到默认值。

---

### 问题 2：缺少数据源配置，应用无法启动（严重 🔴）

5 个环境配置文件（dev/test/prod/docker/my）均未声明 `spring.datasource.*` 属性。MyBatis-Flex starter 会在启动时尝试创建 DataSource Bean，因缺少 JDBC URL 而导致 `ZhushServerApplication` 启动失败。

**修复方案**：在 `application-dev.yaml` 中添加数据源配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhush_dev?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

### 问题 3：缺少 Redis 和 Sa-Token 配置（严重 🔴）

`zhush-spring-boot-starter-redis` 依赖了 `redisson-spring-boot-starter`，`zhush-spring-boot-starter-satoken` 依赖了 `sa-token-redis-template`，但所有环境配置中均无 Redis 连接配置和 Sa-Token 配置。启动时 Redisson 会使用默认地址 `localhost:6379`，如果本地没有 Redis 服务则会连接失败。

**修复方案**：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true
```

---

### 问题 4：mybatis-flex-processor 未加入 annotationProcessorPaths（中等 🟡）

BOM 中声明了 `mybatis-flex-processor`（scope=provided），但根 POM 的 `maven-compiler-plugin` 的 `annotationProcessorPaths` 中并未包含它。当前只有 Lombok、lombok-mapstruct-binding 和 MapStruct-Plus 三个注解处理器。

MyBatis-Flex 的 APT 处理器负责自动生成 TableDef 类（如 `UserTableDef`），用于类型安全的 Lambda 查询。缺失此处理器意味着无法使用 MyBatis-Flex 的 APT 特性。

**修复方案**：在 annotationProcessorPaths 中追加第 4 个处理器：

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>0.2.0</version>
    </path>
    <path>
        <groupId>io.github.linpeilie</groupId>
        <artifactId>mapstruct-plus-processor</artifactId>
        <version>${mapstruct-plus.version}</version>
    </path>
    <!-- 新增 -->
    <path>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-processor</artifactId>
        <version>${mybatis.flex.version}</version>
    </path>
</annotationProcessorPaths>
```

注意：需要在根 POM 的 properties 中添加 `mybatis.flex.version` 属性（当前仅在 zhush-dependencies BOM 中定义），或者在 annotationProcessorPaths 中直接写 `1.11.8`。建议将版本号统一提升至根 POM properties。

---

### 问题 5：mybatis-flex 配置在 5 个环境文件中重复（中等 🟡）

`mapper-locations`、`map-underscore-to-camel-case`、`log-impl` 三项 MyBatis-Flex 配置在 application-dev.yaml、application-test.yaml、application-prod.yaml、application-docker.yaml、application-my.yaml 中完全相同地重复出现。

**修复方案**：将不变的公共配置提取到主 `application.yaml`，环境文件只保留差异项：

```yaml
# application.yaml（公共配置）
mybatis-flex:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# application-dev.yaml（仅差异项）
server:
  port: 9166
mybatis-flex:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
logging:
  level:
    com.zhush: debug
    com.mybatisflex: debug

# application-prod.yaml（生产环境关闭 SQL 日志）
server:
  port: 9166
```

---

### 问题 6：11 个 Starter 模块为空壳（中等 🟡）

除 starter-mybatis 外，其余 12 个 starter 中：

| 状态 | 模块 | 说明 |
|------|------|------|
| 有依赖无代码 | starter-web, starter-redis, starter-satoken, starter-biz-ip, starter-excel, starter-file | POM 中已声明依赖，但无 Java 实现和 AutoConfiguration |
| 无依赖无代码 | starter-biz-data-permission, starter-job, starter-monitor, starter-mq, starter-protection, starter-websocket | 仅有空目录结构和空 imports 文件 |

**建议优先级**（按业务开发依赖排序）：

1. **starter-web**（P0）：全局异常处理器 + 统一响应封装 `CommonResult<T>` + 跨域配置。这是所有 Controller 开发的前提。
2. **starter-satoken**（P0）：登录拦截器 + 权限注解扩展。接口安全的前置条件。
3. **starter-redis**（P1）：Redisson 配置类 + 缓存工具封装 + 分布式锁。
4. **starter-protection**（P2）：限流、幂等、防重复提交。
5. 其余 starter 可按业务需要逐步填充。

---

### 问题 7：SmUtils 使用硬编码默认密钥（中等 🟡）

`SmUtils.java` 中的国密算法密钥采用三级回退策略：系统属性 → 环境变量 → 硬编码默认值。硬编码默认值为：

- SM3 HMAC Key：`"zhush_sm3_default_key"`
- SM4 密钥：`"zhush12345678901"`（16 字节）
- SM2 私钥/公钥：空字符串

这在开发阶段可以接受，但如果忘记在生产环境配置，会导致所有加密数据使用公开可查的默认密钥，构成安全隐患。

**修复方案**：

1. 在生产环境配置中移除默认值回退，强制要求通过环境变量或系统属性提供密钥
2. 添加启动时校验逻辑：如果密钥来自默认值且当前 profile 为 `prod`，抛出异常阻止启动
3. 在配置文件中增加配置项，纳入 `@ConfigurationProperties` 管理：

```java
@ConfigurationProperties(prefix = "zhush.crypto")
public class CryptoProperties {
    private String sm2PrivateKey;
    private String sm2PublicKey;
    private String sm3Key;
    private String sm4Key;
    private boolean allowDefaultKey = false; // 生产环境设为 false
}
```

---

### 问题 8：测试类无法运行（低 🟢）

`ZhushModuleSystemApplicationTests` 和 `ZhushModuleMemberApplicationTests` 使用 `@SpringBootTest`，但对应的包中没有 `@SpringBootApplication` 类。`ZhushServerApplicationTests` 虽然有 `ZhushServerApplication` 可引导上下文，但缺少数据源配置也会启动失败。

**修复方案**：

- 业务模块的测试类改为指向 `ZhushServerApplication` 的上下文：
  ```java
  @SpringBootTest(classes = ZhushServerApplication.class)
  ```
- 或者删除业务模块的测试类，在 `zhush-server/src/test` 中统一管理集成测试
- 添加 `src/test/resources/application-test.yaml` 配置测试专用数据源（如 H2 内存数据库）

---

### 问题 9：zhush-common 依赖过重（低 🟢）

`zhush-common` 作为所有模块的基础层，引入了以下重型依赖：

| 依赖 | 体积/影响 | 是否必要 |
|------|----------|---------|
| `tika-parsers-standard-package` | ~50MB+，包含 PDF/Office/图片等解析器 | 仅文件上传场景需要 |
| `apm-toolkit-trace` | SkyWalking API | 仅生产链路追踪需要 |
| `mapstruct-plus-spring-boot-starter` | 引入 Spring Boot Starter 全家桶 | 仅对象映射场景需要 |
| `bcpkix-jdk18on` | BouncyCastle 加密套件 | 仅国密场景需要 |

这些依赖会传递到每一个业务模块的 classpath 中，增加启动时间和内存占用。

**建议**：将非核心工具拆分为独立模块，或在 `zhush-common` 中将非必要依赖标记为 `<optional>true</optional>`：

```xml
<!-- 仅文件模块需要 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <optional>true</optional>
</dependency>
```

---

### 问题 10：缺少 logback-spring.xml（低 🟢）

项目中没有任何 logback 配置文件。当前仅通过 `logging.level` 控制日志级别，缺少日志输出格式、文件滚动策略、按环境区分日志级别等配置。

**建议**：创建 `zhush-server/src/main/resources/logback-spring.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <springProfile name="dev,my">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="test,prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

### 问题 11：pluginManagement 中的 flatten-maven-plugin 空声明（低 🟢）

根 POM `<pluginManagement>` 中有一个无版本、无配置的 flatten-maven-plugin 声明（第 75-78 行），而下方 `<plugins>` 中又有完整的配置。这个空声明不起任何作用，容易引起混淆。

**修复方案**：将完整配置合并到 `<pluginManagement>` 中，`<plugins>` 中仅保留引用：

```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>flatten-maven-plugin</artifactId>
            <version>${flatten-maven-plugin.version}</version>
            <configuration>
                <flattenMode>oss</flattenMode>
                <updatePomFile>true</updatePomFile>
            </configuration>
            <executions>
                <execution><id>flatten</id><phase>process-resources</phase><goals><goal>flatten</goal></goals></execution>
                <execution><id>flatten.clean</id><phase>clean</phase><goals><goal>clean</goal></goals></execution>
            </executions>
        </plugin>
        <!-- ... -->
    </plugins>
</pluginManagement>

<plugins>
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>flatten-maven-plugin</artifactId>
    </plugin>
</plugins>
```

---

### 问题 12：lombok-mapstruct-binding 版本号硬编码（低 🟢）

annotationProcessorPaths 中 `lombok-mapstruct-binding` 使用硬编码 `0.2.0`，而其余版本均使用 `${...}` 属性。建议统一提取为属性。

---

### 问题 13：缺少 README.md 和 Docker 支持（低 🟢）

项目缺少 README 文档、Dockerfile 和 docker-compose.yml。对于一个多人协作项目，这些是基本要求。

---

## 三、项目健康度评估

| 维度 | 评分 | 说明 |
|------|------|------|
| **模块架构** | ⭐⭐⭐⭐ | 分层清晰，BOM 版本管理集中，模块职责划分合理 |
| **POM 规范** | ⭐⭐⭐⭐ | 版本统一用 ${revision}，依赖集中管理，无明显冲突 |
| **代码实现** | ⭐⭐ | 仅 common 和 starter-mybatis 有实现，其余为空壳 |
| **运行配置** | ⭐⭐ | 有端口和 MyBatis-Flex 配置，缺数据源/Redis/安全配置 |
| **自动配置** | ⭐⭐ | mybatis starter 注册正常，其余 12 个 imports 文件为空 |
| **测试覆盖** | ⭐ | 3 个空壳测试类，无实际测试逻辑 |
| **文档/DevOps** | ⭐ | 无 README、无 Dockerfile、无 CI 配置 |

**综合评估**：项目处于**脚手架后期阶段**，基础设施架构已基本就绪。模块分层和 POM 管理规范，但代码实现度较低，需要集中精力填充核心 Starter（web/satoken/redis）的自动配置和业务模块的首批业务代码。

---

## 四、优先级路线图

### P0 — 阻塞开发（建议立即完成）

| # | 任务 | 预估耗时 |
|---|------|---------|
| 1 | 修复 Listener 中 userId 硬编码 | 15 分钟 |
| 2 | 补充数据源/Redis/Sa-Token 配置 | 30 分钟 |
| 3 | 实现 starter-web 自动配置（全局异常处理 + CommonResult） | 2 小时 |
| 4 | 实现 starter-satoken 自动配置（登录拦截器） | 2 小时 |

### P1 — 进入正式开发前

| # | 任务 | 预估耗时 |
|---|------|---------|
| 5 | mybatis-flex-processor 加入 annotationProcessorPaths | 5 分钟 |
| 6 | 消除 mybatis-flex 配置重复 | 15 分钟 |
| 7 | 实现 starter-redis 自动配置 | 1 小时 |
| 8 | 补充 zhush-common 核心基础（CommonResult/PageResult/ServiceException） | 2 小时 |

### P2 — 迭代优化

| # | 任务 | 预估耗时 |
|---|------|---------|
| 9 | SmUtils 密钥配置化 | 1 小时 |
| 10 | 创建 logback-spring.xml | 30 分钟 |
| 11 | 修复测试类 | 30 分钟 |
| 12 | zhush-common 依赖瘦身（optional 标记） | 1 小时 |

### P3 — 长期建设

| # | 任务 | 预估耗时 |
|---|------|---------|
| 13 | 补充 README.md | 30 分钟 |
| 14 | Docker 化（Dockerfile + docker-compose） | 2 小时 |
| 15 | CI/CD 流水线 | 4 小时 |
| 16 | 填充其余 Starter 实现 | 各 2 小时 |
| 17 | 清理 pluginManagement 空声明 + 属性提取 | 10 分钟 |

---

## 五、Spring Boot 4.x 兼容性评估

| 技术 | 版本 | 兼容性 | 风险 |
|------|------|--------|------|
| MyBatis-Flex | 1.11.8 | ✅ 已适配（boot4-starter） | 低 |
| Sa-Token | 1.45.0 | ✅ 已适配（boot4-starter） | 低 |
| Redisson | 4.6.1 | ⚠️ 需验证（redisson-spring-boot-starter 自动检测） | 中 |
| Knife4j | 4.5.0 | ⚠️ 基于 SpringDoc 3.x，需确认 Boot 4 兼容性 | 中 |
| MapStruct-Plus | 1.5.1 | ✅ 纯编译期，与 Boot 版本无关 | 低 |
| Hutool | 5.8.46 | ✅ 无 Spring 依赖 | 低 |
| BouncyCastle | 1.84 | ✅ 无 Spring 依赖 | 低 |
| SkyWalking | 9.6.0 | ✅ Java Agent 方式，与 Boot 版本无关 | 低 |

建议执行 `mvn dependency:tree -pl zhush-server -Dverbose` 做一次全量依赖树解析，确认无传递冲突。

---

*文档生成时间：2026-07-11*
*分析工具：QoderWork 自动化架构分析*
