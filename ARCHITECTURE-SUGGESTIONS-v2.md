# zhush-boot4-parent 架构分析报告（v2）

> 分析日期：2026-07-11
> 项目版本：1.0.0（${revision}）
> 技术栈：Spring Boot 4.1.0 / Java 21 / MyBatis-Flex 1.11.8 / Sa-Token 1.45.0 / Redisson 4.6.1

---

## 一、上期问题修复追踪

对比 2026-07-09 的 v1 报告，上次提出的 5 个问题修复情况如下：

| 问题 | 状态 | 说明 |
|------|------|------|
| P0：根 POM 未聚合业务模块 | ✅ 已修复 | modules 中已包含 module-system、module-member、zhush-server |
| P0：业务模块版本号脱节 | ✅ 已修复 | 所有模块统一使用 ${revision}（1.0.0） |
| P0：starter-redis parent 异常 | ✅ 已修复 | parent 已改为 zhush-framework |
| P1：Starter 缺少自动配置声明 | ⚠️ 部分修复 | 13 个 AutoConfiguration.imports 文件已创建，但全部为空（0 字节） |
| P2：配置文件极简 | ❌ 未修复 | 7 个 YAML 文件仍仅有 spring.application.name |

---

## 二、本轮新增问题诊断

### 问题 1：AutoConfiguration.imports 全部为空（严重 🔴）

`GlobalFlexAutoConfiguration` 是项目中唯一的 `@AutoConfiguration` 类（位于 starter-mybatis），但其对应的 imports 文件内容为空。Spring Boot 4 不会扫描到该类，导致 MyBatis-Flex 的全局审计字段自动填充（createBy/createTime/updateBy/updateTime）在运行时完全失效。

**影响范围**：所有继承 BaseEntity 的实体在插入和更新时不会自动填充审计字段。

**修复方案**：在以下文件中写入全限定类名：
```
# zhush-spring-boot-starter-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.zhush.mybatis.config.GlobalFlexAutoConfiguration
```

其余 12 个 starter 的 imports 文件可暂留空，待实际开发 AutoConfiguration 类后再填充。

---

### 问题 2：spring-boot-maven-plugin 缺少版本号（严重 🔴）

`zhush-server/pom.xml` 中引用了 `spring-boot-maven-plugin`，但未指定版本号：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <!-- 缺少 <version> -->
</plugin>
```

项目未继承 `spring-boot-starter-parent`，且根 POM 的 `<pluginManagement>` 中也未管理该插件版本。Maven 构建时会回退到仓库中最新或缓存的版本，行为不可预测，可能导致打包失败。

**修复方案**：在根 POM 的 `<pluginManagement>` 中增加版本管理：

```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>${spring.boot.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</pluginManagement>
```

注意 `${spring.boot.version}` 需要在根 POM 的 `<properties>` 中定义（当前仅在 zhush-dependencies 中定义，根 POM 无法引用），或者直接在根 POM properties 中声明该属性。

---

### 问题 3：业务模块零依赖框架 Starter（严重 🔴）

`zhush-module-system` 的 `<dependencies>` 完全为空，`zhush-module-member` 仅引入了 `spring-boot-starter` 和 `spring-boot-starter-test`。两个业务模块都没有引入任何框架 Starter（web、mybatis、redis、satoken 等），这意味着：

- 即使 zhush-server 依赖了这两个模块，它们也不会获得 Web、数据库、缓存、认证等能力
- 业务模块无法编译 Controller、Service、Mapper 等核心类
- 整个依赖链在 starter-mybatis → module-system/member 之间断裂

**修复方案**：业务模块按需引入框架 Starter，推荐的最小依赖集：

```xml
<!-- zhush-module-system/pom.xml 和 zhush-module-member/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.zhush</groupId>
        <artifactId>zhush-spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhush</groupId>
        <artifactId>zhush-spring-boot-starter-mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhush</groupId>
        <artifactId>zhush-spring-boot-starter-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhush</groupId>
        <artifactId>zhush-spring-boot-starter-satoken</artifactId>
    </dependency>

    <!-- test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

### 问题 4：Jackson 2.x/3.x 版本混用（中等 🟡）

`JacksonUtils.java` 同时使用了两个不同大版本的 Jackson API：

- `tools.jackson.databind.*`（Jackson 3.x 新命名空间，由 Spring Boot 4.1.0 引入）
- `com.fasterxml.jackson.annotation.JsonInclude`（Jackson 2.x 旧命名空间）

在 Spring Boot 4.x 环境下，Jackson 3.x（`tools.jackson`）是默认版本，而 Jackson 2.x 的注解包（`com.fasterxml.jackson.annotation`）可能不在 classpath 中或行为不一致，可能导致运行时 `NoClassDefFoundError` 或注解被静默忽略。

**修复方案**：将 `JsonInclude` 的引用替换为 Jackson 3.x 的等价类：

```java
// 替换前（Jackson 2.x）
import com.fasterxml.jackson.annotation.JsonInclude;
// ...
.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))

// 替换后（Jackson 3.x）
import tools.jackson.annotation.JsonInclude;
// ...
.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
```

---

### 问题 5：starter-web 无任何依赖（中等 🟡）

`zhush-spring-boot-starter-web/pom.xml` 没有声明任何依赖，甚至不包括 `spring-boot-starter-web`。作为 Web 层 Starter，它至少需要：

```xml
<dependencies>
    <dependency>
        <groupId>com.zhush</groupId>
        <artifactId>zhush-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- API 文档 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

### 问题 6：Listener 中 userId 硬编码为 1L（中等 🟡）

`GlobalInsertListener.getUserId()` 和 `GlobalUpdateListener.getUserId()` 均硬编码返回 `1L`。这在开发阶段可以理解，但在进入正式开发前需要接入 Sa-Token 获取当前登录用户。

**修复方案**：通过 Sa-Token 获取当前用户 ID，同时处理未登录场景（如系统自动任务）：

```java
private Long getUserId() {
    try {
        return StpUtil.getLoginIdAsLong();
    } catch (Exception e) {
        // 系统级操作或定时任务等无用户上下文的场景
        return 0L;
    }
}
```

注意：由于 starter-mybatis 的 pom.xml 中将 starter-satoken 声明为 `provided` scope，需要在 Listener 中通过反射或可选依赖来处理 Sa-Token 不存在的情况，避免 `NoClassDefFoundError`。

---

### 问题 7：zhush-common 的 Lombok scope 不一致（低 🟢）

`zhush-framework/pom.xml` 将 Lombok 声明为 `provided` scope，但 `zhush-common/pom.xml` 重新声明 Lombok 时未指定 scope，覆盖了父级的 provided。这导致 Lombok 会以 compile scope 传递给所有依赖 zhush-common 的模块。

**影响**：不会导致功能问题，但 Lombok 会出现在最终打包中，属于不必要的传递依赖。

**修复方案**：在 zhush-common 中显式指定 scope：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

---

### 问题 8：缺少 spring.profiles.active 配置（低 🟢）

`application.yaml` 中未设置 `spring.profiles.active`，导致 dev/test/prod/docker 环境配置文件不会被激活。虽然可以通过命令行参数 `--spring.profiles.active=dev` 指定，但建议在主配置文件中设置默认值：

```yaml
spring:
  profiles:
    active: dev
```

---

### 问题 9：业务模块测试类缺少启动类（低 🟢）

`ZhushModuleSystemApplicationTests` 和 `ZhushModuleMemberApplicationTests` 使用 `@SpringBootTest` 注解，但对应的包下没有 `@SpringBootApplication` 启动类，测试无法正确加载上下文。

建议将这两个测试类的测试上下文指向 `zhush-server` 的启动类，或者在业务模块中不写 `@SpringBootTest` 测试，仅保留单元测试。

---

### 问题 10：zhush-common 引入重型依赖（低 🟢）

`zhush-common` 作为所有模块的基础层，引入了以下重型依赖：

- `tika-parsers-standard-package`：文档内容解析（PDF/Office/图片等），体积较大
- `apm-toolkit-trace`：SkyWalking 链路追踪
- `mapstruct-plus-spring-boot-starter`：对象映射框架
- `bcpkix-jdk18on`：BouncyCastle 加密套件

这些依赖并非所有场景都需要，会导致每个模块的 classpath 都膨胀。建议将非核心工具类拆分为独立模块（如 `zhush-common-crypto`、`zhush-common-doc`），或至少将 tika-parsers 改为 optional。

---

## 三、依赖链完整性分析

当前项目的依赖链存在多处断裂，下图展示了理想状态 vs 当前状态：

```
【理想依赖链】
zhush-server
  ├── zhush-module-system
  │     ├── starter-web（spring-boot-starter-web + knife4j）
  │     ├── starter-mybatis（mybatis-flex + 多数据库驱动）
  │     │     └── zhush-common
  │     ├── starter-redis（redisson）
  │     └── starter-satoken（sa-token + redis-template）
  └── zhush-module-member
        └── （同上）

【当前实际状态】
zhush-server
  ├── zhush-module-system      ← 零依赖 ❌
  └── zhush-module-member      ← 仅 spring-boot-starter ❌
```

---

## 四、Spring Boot 4.x 兼容性风险提示

Spring Boot 4.1.0 是一个较新的版本，生态兼容性需要持续关注：

| 技术 | 版本 | 兼容性评估 |
|------|------|-----------|
| MyBatis-Flex | 1.11.8 | 已适配 Boot 4（使用 `mybatis-flex-spring-boot4-starter`），风险低 |
| Sa-Token | 1.45.0 | 已适配 Boot 4（使用 `sa-token-spring-boot4-starter`），风险低 |
| Redisson | 4.6.1 | 使用 `redisson-spring-boot-starter`，需确认对 Boot 4 的兼容性，风险中 |
| Knife4j | 4.5.0 | 基于 SpringDoc 3.x，需确认 Boot 4 兼容性，风险中 |
| MapStruct-Plus | 1.5.1 | 与 Spring Boot 版本关联较弱（纯编译期），风险低 |
| Hutool | 5.8.46 | 无 Spring 依赖，风险低 |
| BouncyCastle | 1.84 | 无 Spring 依赖，风险低 |
| SkyWalking | 9.6.0 | Java Agent 方式挂载，与 Boot 版本关联弱，风险低 |

建议执行 `mvn dependency:tree -Dverbose` 做一次全量依赖树解析，确认无版本冲突。

---

## 五、优先级路线图

### P0 — 必须立即修复（阻塞开发）

| # | 问题 | 修复工作量 |
|---|------|-----------|
| 1 | AutoConfiguration.imports 为空（starter-mybatis） | 1 分钟 |
| 2 | spring-boot-maven-plugin 缺少版本号 | 5 分钟 |
| 3 | 业务模块零依赖框架 Starter | 10 分钟 |

### P1 — 进入正式开发前修复

| # | 问题 | 修复工作量 |
|---|------|-----------|
| 4 | Jackson 2.x/3.x 版本混用 | 5 分钟 |
| 5 | starter-web 无任何依赖 | 10 分钟 |
| 6 | Listener 中 userId 硬编码 | 15 分钟 |
| 7 | 缺少 spring.profiles.active | 1 分钟 |
| 8 | 建立最小可用的多环境配置（数据库/Redis/端口） | 30 分钟 |

### P2 — 迭代优化

| # | 问题 | 修复工作量 |
|---|------|-----------|
| 9 | zhush-common Lombok scope 不一致 | 1 分钟 |
| 10 | 业务模块测试类缺少启动类 | 10 分钟 |
| 11 | zhush-common 重型依赖拆分 | 2 小时 |
| 12 | 为 starter-web 建立全局异常处理 + 统一响应封装 | 2 小时 |
| 13 | 为 starter-redis 建立 AutoConfiguration | 1 小时 |
| 14 | 为 starter-satoken 建立 AutoConfiguration + 拦截器 | 2 小时 |

### P3 — 长期建设

| # | 问题 | 修复工作量 |
|---|------|-----------|
| 15 | 补充 README.md（项目背景/启动步骤/模块说明） | 30 分钟 |
| 16 | Docker 化支持（Dockerfile + docker-compose） | 2 小时 |
| 17 | CI/CD 流水线（GitHub Actions / GitLab CI） | 4 小时 |
| 18 | 补充其余 Starter 自动配置（job/mq/monitor/websocket） | 各 2 小时 |
| 19 | 集成测试框架（Testcontainers + 多数据库矩阵） | 4 小时 |

---

## 六、核心配置模板（建议直接使用）

### application.yaml（主配置）

```yaml
spring:
  profiles:
    active: dev
  application:
    name: zhush-server

server:
  port: 8080

mybatis-flex:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### application-dev.yaml（开发环境）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhush_dev?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

logging:
  level:
    com.zhush: debug
    com.mybatisflex: debug
```

---

## 七、修复验证清单

完成 P0/P1 修复后，按以下步骤验证：

1. **构建验证**：在项目根目录执行 `mvn clean install -DskipTests`，确认无构建错误
2. **依赖树检查**：执行 `mvn dependency:tree -pl zhush-server`，确认所有 Starter 出现在依赖树中
3. **启动验证**：运行 `ZhushServerApplication.main()`，确认 Spring Boot 正常启动且无 BeanCreation 异常
4. **自动配置验证**：启动时观察日志中的 `CONDITIONS EVALUATION REPORT`，确认 `GlobalFlexAutoConfiguration` 出现在 "Positive matches" 中
5. **数据库验证**：配置数据源后，执行一条简单的 Mapper 查询，确认审计字段自动填充生效

---

*文档生成时间：2026-07-11*
*分析工具：QoderWork 自动化架构分析*
