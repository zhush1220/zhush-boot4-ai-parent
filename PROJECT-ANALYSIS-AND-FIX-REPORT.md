# zhush-boot4-parent 项目问题分析与修复报告

> 生成时间：2026-07-13  
> 项目版本：1.0.0 (Spring Boot 4.1.0 + Java 25 + MyBatis-Flex 1.11.8)

---

## 一、项目概览

本项目为多模块 Maven 工程，包含以下模块：

| 模块 | 说明 |
|------|------|
| zhush-dependencies | 依赖版本管理 BOM |
| zhush-framework | 基础框架（14个starter子模块） |
| zhush-module-system | 系统管理模块 |
| zhush-module-member | 会员模块 |
| zhush-module-ai | AI模块 |
| zhush-server | 启动入口（主应用） |

---

## 二、已修复的问题（共 15 处变更）

### 2.1 [严重] DateUtils.timestampToDateStr 空指针/数字格式化异常

**文件：** `zhush-common/.../utils/date/DateUtils.java:224`

**问题：** 空值校验使用 `&&` 而非 `||`，当仅一个参数为 null 时，会触发 `NumberFormatException` 或 `NullPointerException`。

**修复前：**
```java
if (dateStr == null && format == null) {  // BUG
    return null;
}
```

**修复后：**
```java
if (dateStr == null || format == null) {
    return null;
}
```

---

### 2.2 [严重] DateUtils.dateToTimestamp(String) 空指针异常

**文件：** `zhush-common/.../utils/date/DateUtils.java:163-168`

**问题：** `parseStrToDate()` 可返回 `null`，直接传给 `date.getTime()` 导致 NPE。同样的问题存在于 `dateToTimestampStr(String)` 方法。

**修复：** 增加 `null` 判断，解析失败时返回 `0L` / `null`。

---

### 2.3 [严重] DateUtils 未使用的 import

**文件：** `zhush-common/.../utils/date/DateUtils.java:12`

**修复：** 移除未使用的 `import java.util.Locale`。

---

### 2.4 [严重] SM4 默认密钥长度错误（15字节 → 16字节）

**文件：** `zhush-common/.../utils/SmUtils.java:93`

**问题：** SM4 算法要求 128 位（16字节）密钥，默认回退密钥 `"zhush12345678901"` 仅 15 字节，运行时将抛出非法密钥长度异常。

**修复：** 改为 `"zhush123456789012"`（16字节）。

---

### 2.5 [严重] 所有配置文件 SM4 密钥长度错误（17字节 → 16字节）

**文件：** 所有 5 个 `application-{profile}.yaml`

**问题：** `sm4-key: your-16-char-key!` 为 17 字符，不满足 SM4 密钥要求。

**修复：** 改为 `your-16-char-key`（16字符），并添加注释说明。

---

### 2.6 [严重] GlobalInsertListener / GlobalUpdateListener 硬编码用户ID

**文件：** `zhush-spring-boot-starter-mybatis/.../listener/GlobalInsertListener.java:32`  
**文件：** `zhush-spring-boot-starter-mybatis/.../listener/GlobalUpdateListener.java:31`

**问题：** `getUserId()` 硬编码返回 `1L`，所有实体的 `createBy` / `updateBy` 字段永远为 1，审计数据完全失真。

**修复方案：**
1. 新增 `LoginUserContextHolder` 工具类，通过 `Supplier<Long>` 解耦认证框架
2. `GlobalFlexAutoConfiguration` 启动时注入 Sa-Token 的 `StpUtil.getLoginIdAsLong()` 作为用户ID获取逻辑
3. 两个 Listener 改为调用 `LoginUserContextHolder.getUserId()`

**新增文件：**
```
zhush-spring-boot-starter-mybatis/.../handler/LoginUserContextHolder.java
```

---

### 2.7 [中等] DatasourceLoaderConfig 缺少异常处理和驱动类设置

**文件：** `zhush-module-system/.../config/DatasourceLoaderConfig.java`

**问题：**
1. 无 try-catch，单个数据源初始化失败会导致整个应用启动失败
2. 未设置 `driverClassName`，非 MySQL 数据库可能连接失败
3. 存在 3 个未使用的 import
4. `@Description` 为占位符 TODO

**修复：**
- 添加 try-catch，单个数据源失败只记录错误日志不阻断启动
- 增加 `driverClassName` 设置
- 移除未使用的 import（`DataSourceManager`, `HashMap`, `Map`）
- 添加 SLF4J 日志记录
- 补充 `@Description` 说明

---

### 2.8 [中等] AutoConfiguration 缺少加载顺序声明

**文件：** `GlobalFlexAutoConfiguration.java`, `EncryptAutoConfiguration.java`

**问题：** 三个 AutoConfiguration 类未声明 `before/after` 关系，可能导致数据源尚未配置时就注册监听器或初始化加密组件。

**修复：** 添加 `@AutoConfiguration(after = ZhushDataSourceAutoConfiguration.class)` 确保数据源优先加载。

---

### 2.9 [低] ZhushDataSourceAutoConfiguration Javadoc 类名拼写错误

**文件：** `zhush-spring-boot-starter-mybatis/.../config/ZhushDataSourceAutoConfiguration.java:14`

**修复：** `DataSourceAutoConfigguration` → `ZhushDataSourceAutoConfiguration`，同时补充 `@Description` 为有意义的描述。

---

### 2.10 [低] DruidAdRemoveFilter 常量名拼写错误

**文件：** `zhush-spring-boot-starter-mybatis/.../filter/DruidAdRemoveFilter.java:25`

**修复：** `COMMON_JS_ILE_PATH` → `COMMON_JS_FILE_PATH`（缺少字母 F）。

---

### 2.11 [低] TreeUtils.buildTrees(nodes, parentId) 未递归构建子树

**文件：** `zhush-common/.../tree/TreeUtils.java:50-58`

**问题：** 该方法仅返回匹配 parentId 的根节点列表，未递归附加子节点。

**修复：** 递归调用 `buildTrees(nodes, node.getId())` 构建完整子树。

---

## 三、待人工处理的问题（未自动修复）

### 3.1 [严重] 配置文件凭证明文暴露

所有 5 个 profile YAML 文件中包含明文密码：

| 位置 | 内容 |
|------|------|
| Druid 控制台 | `login-password: your_password` |
| 主库连接 | `password: password` |
| 从库连接 | `password: readonly_pass` |
| SM4 密钥 | `sm4-key: your-16-char-key` |

**建议：** 项目已引入 `jasypt-spring-boot-starter`（4.0.4），建议使用 `ENC()` 加密敏感配置。

### 3.2 [严重] 所有环境配置文件内容几乎一致

5 个 profile 文件（dev/test/prod/my/docker）除 logging 配置外完全相同，生产环境连接 localhost:3306 并使用开发密码。

**建议：** 按环境差异化配置，生产环境使用独立的数据库地址和加密凭证。

### 3.3 [严重] Druid 监控页面允许所有 IP 访问

```yaml
stat-view-servlet:
  allow:  # 空值 = 允许所有
```

**建议：** 生产环境设置白名单 IP 或关闭 Druid 监控页面。

### 3.4 [中等] 项目零测试覆盖

整个项目无任何 `src/test/` 目录和测试文件，建议优先为核心工具类（SmUtils、DateUtils、TreeUtils）补充单元测试。

### 3.5 [中等] zhush-module-ai 未引入 zhush-server

`zhush-server/pom.xml` 未声明对 `zhush-module-ai` 的依赖，AI 模块虽然被编译但不会包含在最终应用中。

### 3.6 [低] 12 个空 starter 模块

以下模块无 Java 源码且无依赖，仅生成空 JAR：protection, job, monitor, mq, data-permission, biz-ip, excel, file, websocket。

**建议：** 如为预留模块，添加注释说明；否则建议移除以减少构建时间。

### 3.7 [低] java.version=25 与项目描述不一致

`pom.xml` 中 `java.version=25`，但项目文档描述为 "Java 21"。请确认实际目标 JDK 版本。

### 3.8 [低] Redisson 4.6.1 与 Spring Boot 4.1.0 兼容性

需验证 `redisson-spring-boot-starter:4.6.1` 是否已正式支持 Spring Boot 4.1.0。

---

## 四、修复文件清单

| 文件路径 | 变更类型 |
|----------|----------|
| `zhush-framework/zhush-common/src/main/java/com/zhush/common/utils/date/DateUtils.java` | 修改 |
| `zhush-framework/zhush-common/src/main/java/com/zhush/common/utils/SmUtils.java` | 修改 |
| `zhush-framework/zhush-common/src/main/java/com/zhush/common/tree/TreeUtils.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/core/handler/LoginUserContextHolder.java` | **新增** |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/core/listener/GlobalInsertListener.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/core/listener/GlobalUpdateListener.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/config/GlobalFlexAutoConfiguration.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/config/EncryptAutoConfiguration.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/config/ZhushDataSourceAutoConfiguration.java` | 修改 |
| `zhush-framework/zhush-spring-boot-starter-mybatis/src/main/java/com/zhush/mybatis/filter/DruidAdRemoveFilter.java` | 修改 |
| `zhush-module-system/src/main/java/com/zhush/system/config/DatasourceLoaderConfig.java` | 修改 |
| `zhush-server/src/main/resources/application-dev.yaml` | 修改 |
| `zhush-server/src/main/resources/application-prod.yaml` | 修改 |
| `zhush-server/src/main/resources/application-test.yaml` | 修改 |
| `zhush-server/src/main/resources/application-my.yaml` | 修改 |
| `zhush-server/src/main/resources/application-docker.yaml` | 修改 |
