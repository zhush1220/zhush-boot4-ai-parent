# MyBatis-Flex + Druid 多数据源动态切换方案

> 基于 zhush-boot4-parent 项目（Spring Boot 4.1.0 + MyBatis-Flex 1.11.8 + Druid 1.2.28）

## 一、方案概述

MyBatis-Flex 内置了多数据源支持，核心类是 `FlexDataSource`（继承自 `AbstractRoutingDataSource`），无需引入额外的 `dynamic-datasource-spring-boot-starter`。结合项目已有的 Druid 连接池，本方案实现：

1. **YAML 声明式配置**多个数据源（`mybatis-flex.datasource` 前缀）
2. **`@UseDataSource` 注解**在 Mapper / Service / Controller 层声明式切换
3. **`DataSourceKey.use()` API** 编程式动态切换
4. **运行时动态添加/移除数据源**（多租户、动态业务库场景）
5. **全局可配置化**（数据源元信息存库，启动时自动挂载）

---

## 二、配置变更

### 2.1 替换数据源配置（application-*.yaml）

将原来的 `spring.datasource` 配置迁移到 `mybatis-flex.datasource` 下，每个数据源以 key 命名：

```yaml
# ===================== 多数据源配置 =====================
mybatis-flex:
  datasource:
    # --- 主库（默认数据源）---
    master:
      type: com.alibaba.druid.pool.DruidDataSource
      url: jdbc:mysql://localhost:3306/zhush_main?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: password
      # Druid 连接池属性（直接写 setter 对应的 key）
      initialSize: 5
      maxActive: 20
      minIdle: 5
      maxWait: 60000
      validationQuery: SELECT 1
      testWhileIdle: true

    # --- 从库（只读/报表库）---
    slave:
      type: com.alibaba.druid.pool.DruidDataSource
      url: jdbc:mysql://localhost:3306/zhush_slave?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: readonly
      password: readonly_pass
      initialSize: 3
      maxActive: 10
      minIdle: 3
      maxWait: 60000
      validationQuery: SELECT 1
      testWhileIdle: true

    # --- 业务库 B（跨库业务）---
    business-b:
      type: com.alibaba.druid.pool.DruidDataSource
      url: jdbc:mysql://192.168.1.100:3306/zhush_biz_b?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: biz_b_pass
      initialSize: 3
      maxActive: 15
      minIdle: 2
      maxWait: 60000
      validationQuery: SELECT 1
      testWhileIdle: true

  # MyBatis-Flex 其他配置保持不变
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

**关键说明：**

- `type` 指定为 `com.alibaba.druid.pool.DruidDataSource`，MyBatis-Flex 会自动识别并用 setter 注入下面的属性。
- 第一个数据源（`master`）自动成为**默认数据源**，不带 `@UseDataSource` 注解的 Mapper/Service 都走它。
- Druid 监控（`stat-view-servlet`、`filter`）的配置仍然放在 `spring.datasource.druid` 下，由 `druid-spring-boot-4-starter` 管理。

### 2.2 Druid 全局监控配置（保留在 spring.datasource.druid）

```yaml
spring:
  datasource:
    druid:
      # 全局 Druid 监控配置（对所有数据源生效）
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 200
          merge-sql: true
        wall:
          enabled: true
          config:
            multi-statement-allow: true
      stat-view-servlet:
        enabled: true
        login-username: admin
        login-password: your_password
        url-pattern: /druid/*
```

---

## 三、声明式数据源切换

MyBatis-Flex 提供 `@UseDataSource` 注解，可以在四个层级使用，优先级从高到低：

| 优先级 | 使用位置 | 说明 |
|--------|---------|------|
| 1 (最高) | `DataSourceKey.use()` | 编程式切换，适合动态场景 |
| 2 | `@UseDataSource` 在方法上 | 方法级别，最细粒度 |
| 3 | `@UseDataSource` 在类上 | 整个 Mapper / Service 类 |
| 4 (最低) | `@Table(dataSource="xxx")` | 实体类绑定默认数据源 |

### 3.1 Mapper 层注解

```java
package com.zhush.module.system.mapper;

import com.mybatisflex.annotation.UseDataSource;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper —— 使用默认数据源 (master)
 */
@Mapper
public interface SysUserMapper extends BaseMapperX<SysUser> {
    // 默认走 master，不需要额外注解
}
```

```java
package com.zhush.module.system.mapper;

import com.mybatisflex.annotation.UseDataSource;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.module.system.entity.ReportData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报表 Mapper —— 整个类走 slave 数据源
 */
@Mapper
@UseDataSource("slave")
public interface ReportDataMapper extends BaseMapperX<ReportData> {
    // 所有方法都走 slave
}
```

```java
package com.zhush.module.system.mapper;

import com.mybatisflex.annotation.UseDataSource;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.module.system.entity.BizOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务订单 Mapper —— 方法级别切换数据源
 */
@Mapper
public interface BizOrderMapper extends BaseMapperX<BizOrder> {

    // 默认走 master

    /**
     * 查询业务库B的订单
     */
    @UseDataSource("business-b")
    default List<BizOrder> selectFromBizB() {
        return selectListByQuery(QueryWrapper.create());
    }
}
```

### 3.2 Service 层注解

```java
package com.zhush.module.system.service;

import com.mybatisflex.annotation.UseDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportDataMapper reportDataMapper;

    public ReportService(ReportDataMapper reportDataMapper) {
        this.reportDataMapper = reportDataMapper;
    }

    /**
     * 类级别已经标注了 @UseDataSource("slave")，这里也可以方法级别覆盖
     */
    @UseDataSource("slave")
    @Transactional(readOnly = true)
    public List<ReportData> getMonthlyReport() {
        return reportDataMapper.selectListByQuery(
            QueryWrapper.create().where(/* ... */)
        );
    }
}
```

### 3.3 实体类绑定数据源

```java
package com.zhush.module.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;

/**
 * 订单实体 —— 默认绑定 business-b 数据源
 */
@Table(value = "biz_order", dataSource = "business-b")
public class BizOrder extends BaseEntity {
    private String orderNo;
    private BigDecimal amount;
    // ...
}
```

---

## 四、编程式动态切换

适用于多租户、请求头路由、AOP 切面等动态场景。

### 4.1 DataSourceKey 工具类

```java
import com.mybatisflex.core.datasource.DataSourceKey;

// 方式一：try-finally 模式（推荐）
try {
    DataSourceKey.use("slave");
    List<Row> rows = Db.selectAll("tb_account");
} finally {
    DataSourceKey.clear(); // 必须清理，防止线程池复用导致数据源泄漏
}

// 方式二：配合 try-with-resources（MyBatis-Flex 提供了 DataSourceKey.AutoClear）
try (var ignored = DataSourceKey.useAndAutoClear("slave")) {
    List<Row> rows = Db.selectAll("tb_account");
}

// 方式三：通配符随机选择（负载均衡场景）
try {
    DataSourceKey.use("slave*"); // 随机选择 slave 开头的数据源
    // ...
} finally {
    DataSourceKey.clear();
}
```

### 4.2 多租户数据源路由（AOP 切面示例）

```java
package com.zhush.mybatis.core.datasource;

import com.mybatisflex.core.datasource.DataSourceKey;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 多租户数据源自动路由切面
 * <p>
 * 从请求头 X-Tenant-Id 中提取租户标识，自动切换到对应的数据源。
 */
@Aspect
@Component
public class TenantDataSourceAspect {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Before("@within(org.springframework.web.bind.annotation.RestController) " +
            "|| @within(org.springframework.stereotype.Controller)")
    public void before() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            // 约定数据源 key 格式：tenant_{tenantId}
            DataSourceKey.use("tenant_" + tenantId);
        }
    }

    @After("@within(org.springframework.web.bind.annotation.RestController) " +
           "|| @within(org.springframework.stereotype.Controller)")
    public void after() {
        DataSourceKey.clear();
    }
}
```

---

## 五、运行时动态添加/移除数据源

适用于多租户动态注册、管理后台添加数据源等场景。

### 5.1 DataSourceManager 服务类

```java
package com.zhush.mybatis.core.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.FlexDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源动态管理器
 * <p>
 * 支持在应用运行时动态添加、移除、查询数据源。
 * 内部持有 FlexDataSource 引用，线程安全。
 */
@Component
public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);

    /**
     * 动态数据源注册表（用于跟踪和清理）
     */
    private final Set<String> dynamicDataSources = ConcurrentHashMap.newKeySet();

    /**
     * 获取 FlexDataSource 实例
     */
    private FlexDataSource getFlexDataSource() {
        FlexDataSource ds = FlexGlobalConfig.getDefaultConfig().getDataSource();
        if (ds == null) {
            throw new IllegalStateException("FlexDataSource 未初始化，请确保应用已完全启动");
        }
        return ds;
    }

    /**
     * 动态添加 Druid 数据源
     *
     * @param name     数据源名称（唯一标识）
     * @param url      JDBC URL
     * @param username 用户名
     * @param password 密码
     */
    public void addDruidDataSource(String name, String url, String username, String password) {
        addDruidDataSource(name, url, username, password, null);
    }

    /**
     * 动态添加 Druid 数据源（带自定义属性）
     *
     * @param name       数据源名称
     * @param url        JDBC URL
     * @param username   用户名
     * @param password   密码
     * @param properties 额外的 Druid 属性（可为 null）
     */
    public void addDruidDataSource(String name, String url, String username, String password,
                                   Map<String, String> properties) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 默认连接池参数
        dataSource.setInitialSize(3);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);

        // 覆盖自定义属性
        if (properties != null) {
            String val;
            if ((val = properties.get("initialSize")) != null) dataSource.setInitialSize(Integer.parseInt(val));
            if ((val = properties.get("maxActive")) != null) dataSource.setMaxActive(Integer.parseInt(val));
            if ((val = properties.get("minIdle")) != null) dataSource.setMinIdle(Integer.parseInt(val));
        }

        try {
            dataSource.init();
        } catch (SQLException e) {
            throw new RuntimeException("数据源 [" + name + "] 初始化失败: " + e.getMessage(), e);
        }

        getFlexDataSource().addDataSource(name, dataSource);
        dynamicDataSources.add(name);
        log.info("动态数据源 [{}] 注册成功，URL: {}", name, url);
    }

    /**
     * 移除动态数据源
     *
     * @param name 数据源名称
     */
    public void removeDataSource(String name) {
        DataSource removed = getFlexDataSource().removeDataSource(name);
        if (removed instanceof DruidDataSource druid) {
            druid.close(); // 释放连接池资源
        }
        dynamicDataSources.remove(name);
        log.info("动态数据源 [{}] 已移除", name);
    }

    /**
     * 查询所有已注册的数据源名称
     */
    public Set<String> getDataSourceNames() {
        return getFlexDataSource().getDataSourceMap().keySet();
    }

    /**
     * 查询所有动态注册的数据源名称
     */
    public Set<String> getDynamicDataSourceNames() {
        return Set.copyOf(dynamicDataSources);
    }

    /**
     * 判断数据源是否存在
     */
    public boolean containsDataSource(String name) {
        return getFlexDataSource().getDataSourceMap().containsKey(name);
    }
}
```

### 5.2 数据源元信息管理（存库 + 启动自动挂载）

将数据源配置存入数据库表，应用启动时自动挂载。

#### 数据源配置表 DDL

```sql
CREATE TABLE `sys_datasource` (
  `id`           BIGINT       NOT NULL COMMENT '主键(雪花ID)',
  `name`         VARCHAR(64)  NOT NULL COMMENT '数据源名称(唯一标识)',
  `url`          VARCHAR(512) NOT NULL COMMENT 'JDBC URL',
  `username`     VARCHAR(128) NOT NULL COMMENT '用户名',
  `password`     VARCHAR(256) NOT NULL COMMENT '密码(加密存储)',
  `driver_class` VARCHAR(128) DEFAULT NULL COMMENT '驱动类名(可选)',
  `pool_type`    VARCHAR(32)  DEFAULT 'druid' COMMENT '连接池类型: druid/hikari',
  `initial_size` INT          DEFAULT 3 COMMENT '初始连接数',
  `max_active`   INT          DEFAULT 10 COMMENT '最大连接数',
  `min_idle`     INT          DEFAULT 2 COMMENT '最小空闲连接数',
  `max_wait`     INT          DEFAULT 60000 COMMENT '获取连接最大等待时间(ms)',
  `status`       TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `remark`       VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `create_by`    BIGINT       DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT       DEFAULT NULL COMMENT '更新人',
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete`    TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';
```

#### 启动时自动挂载

```java
package com.zhush.mybatis.core.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 启动时从数据库加载数据源配置并动态注册
 */
@Configuration
public class DataSourceAutoLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSourceAutoLoader.class);

    private final SysDatasourceMapper datasourceMapper;
    private final DataSourceManager dataSourceManager;

    public DataSourceAutoLoader(SysDatasourceMapper datasourceMapper,
                                DataSourceManager dataSourceManager) {
        this.datasourceMapper = datasourceMapper;
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SysDatasourceDO> list = datasourceMapper.selectEnabledList();
        for (SysDatasourceDO ds : list) {
            try {
                dataSourceManager.addDruidDataSource(
                    ds.getName(),
                    ds.getUrl(),
                    ds.getUsername(),
                    ds.getPassword(),  // 实际使用时需要先解密
                    null
                );
                log.info("自动挂载数据源 [{}] 成功", ds.getName());
            } catch (Exception e) {
                log.error("自动挂载数据源 [{}] 失败: {}", ds.getName(), e.getMessage());
            }
        }
        log.info("数据源自动挂载完成，共加载 {} 个", list.size());
    }
}
```

---

## 六、DataSourceMissingHandler（数据源缺失兜底）

当 `@UseDataSource` 指定的数据源不存在时，可以通过 `DataSourceMissingHandler` 自定义兜底策略（比如动态创建、回退到默认数据源、或抛出异常）。

```java
package com.zhush.mybatis.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.DataSourceKey;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 注册数据源缺失处理器
 */
@Component
public class DataSourceMissingHandlerSetup implements ApplicationRunner {

    private final DataSourceManager dataSourceManager;

    public DataSourceMissingHandlerSetup(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        FlexGlobalConfig.getDefaultConfig().setDataSourceMissingHandler((flexDataSource, dsKey) -> {
            // 策略一：尝试从数据库动态加载
            // SysDatasourceDO config = loadFromDb(dsKey);
            // if (config != null) { dataSourceManager.addDruidDataSource(...); return; }

            // 策略二：回退到默认数据源（记录告警日志）
            org.slf4j.LoggerFactory.getLogger("DataSourceMissingHandler")
                .warn("数据源 [{}] 不存在，已回退到默认数据源", dsKey);
            // 不做任何操作即回退到默认数据源
        });
    }
}
```

---

## 七、事务管理注意事项

多数据源场景下的事务管理有明确的边界限制：

### 7.1 单数据源事务（常规场景）

`@Transactional` 注解正常工作在当前数据源上，不需要额外配置：

```java
@Service
@UseDataSource("master")
public class OrderService {

    @Transactional
    public void createOrder(Order order) {
        orderMapper.insert(order);
        orderDetailMapper.insertBatch(order.getDetails());
    }
}
```

### 7.2 跨数据源事务（分布式事务）

`@Transactional` **不支持跨数据源**。如果业务需要跨多个数据源的一致性，需要引入分布式事务方案（如 Seata）。MyBatis-Flex 支持 Seata AT 模式集成。

### 7.3 关键原则

- 同一个 `@Transactional` 方法内，**只能操作一个数据源**。
- 如果方法内通过 `DataSourceKey.use()` 切换了数据源，`@Transactional` 只作用于切换前的数据源。
- 建议：不同数据源的操作拆分到不同的 Service 方法中，各自管理事务。

---

## 八、需要修复的现有问题

在实施多数据源方案前，需要先修复 `DataSourceAutoConfigguration` 的两个问题：

### 8.1 修复类名拼写错误 + 注册到 AutoConfiguration

```java
// 重命名：DataSourceAutoConfigguration → DataSourceAutoConfiguration
// 添加到 AutoConfiguration.imports：
// com.zhush.mybatis.config.DataSourceAutoConfiguration
```

### 8.2 修正后的 DataSourceAutoConfiguration

```java
package com.zhush.mybatis.config;

import com.alibaba.druid.spring.boot4.autoconfigure.properties.DruidStatProperties;
import com.zhush.mybatis.filter.DruidAdRemoveFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@AutoConfiguration
@EnableConfigurationProperties(DruidStatProperties.class)
@EnableTransactionManagement(proxyTargetClass = true)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true")
    public FilterRegistrationBean<DruidAdRemoveFilter> druidAdRemoveFilter(DruidStatProperties properties) {
        DruidStatProperties.StatViewServlet config = properties.getStatViewServlet();
        String pattern = config.getUrlPattern() != null ? config.getUrlPattern() : "/druid/*";
        String commonJsPattern = pattern.replaceAll("\\*", "js/common.js");
        FilterRegistrationBean<DruidAdRemoveFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DruidAdRemoveFilter());
        registrationBean.addUrlPatterns(commonJsPattern);
        return registrationBean;
    }
}
```

### 8.3 更新 AutoConfiguration.imports

```
com.zhush.mybatis.config.DataSourceAutoConfiguration
com.zhush.mybatis.config.GlobalFlexAutoConfiguration
com.zhush.mybatis.config.EncryptAutoConfiguration
```

---

## 九、完整文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `application-*.yaml` | 修改 | 数据源配置从 `spring.datasource` 迁移到 `mybatis-flex.datasource` |
| `DataSourceAutoConfigguration.java` | 重命名+修复 | 修正拼写错误 |
| `AutoConfiguration.imports` | 修改 | 添加 `DataSourceAutoConfiguration` |
| `DataSourceManager.java` | 新增 | 动态数据源管理器 |
| `TenantDataSourceAspect.java` | 新增 | 多租户路由切面（可选） |
| `DataSourceAutoLoader.java` | 新增 | 启动自动挂载（可选） |
| `DataSourceMissingHandlerSetup.java` | 新增 | 缺失数据源兜底（可选） |
| `sys_datasource` DDL | 新增 | 数据源配置表（可选） |

---

## 十、推荐的使用模式总结

| 场景 | 推荐方式 | 示例 |
|------|---------|------|
| 固定多数据源（主从分离） | YAML 配置 + `@UseDataSource` 注解 | master/slave |
| 按模块绑定数据源 | `@UseDataSource` 在 Mapper 类上 | 报表模块走 slave |
| 按实体绑定数据源 | `@Table(dataSource="xxx")` | 订单实体走 business-b |
| 多租户动态路由 | AOP 切面 + `DataSourceKey.use()` | 从请求头提取租户ID |
| 管理后台动态配置 | 数据库存储 + `DataSourceManager` | 运行时增删数据源 |
| 编程式临时切换 | `DataSourceKey.use()` + try-finally | 定时任务跨库查询 |
