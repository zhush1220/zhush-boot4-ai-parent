# MyBatis-Flex SQL 审计落库设计方案

> 环境：Spring Boot 4.1.0 / JDK 21 / MyBatis-Flex 1.11.8 / Sa-Token / MySQL 8.0+
> 状态：**设计稿，代码暂不实现**。文中 Java 代码均为骨架示意，用于后续开发对齐。
> 目标：启用 MyBatis-Flex 内置 SQL 审计，把每次执行的 SQL 记录到数据库表，并支持管理端查询。

---

## 一、背景与目标

### 1.1 现有日志体系

| 日志 | 表 | 采集方式 | 维度 | 说明 |
| --- | --- | --- | --- | --- |
| 操作日志 | `sys_operate_log` | `@OperateLog` + AOP | HTTP 请求/业务动作 | 记录谁、对什么模块、做了什么操作 |
| AI 审计日志 | `agent_audit_log` | 事件总线监听 | AI 会话生命周期 | 记录 Agent 会话次数、Token、费用等 |

这两类日志都停留在「业务/请求」层面，缺少 **SQL 层**的可观测性：

- 某次业务操作到底执行了哪些 SQL？
- 查询/写入耗时多少？返回了多少行？
- 是否出现慢 SQL、执行次数异常、参数异常？

### 1.2 目标

引入 MyBatis-Flex 自带的 **SQL 审计（Audit）** 能力，把每次执行的 SQL（含 SQL 文本、参数、耗时、数据源、调用语句 ID 等）异步、批量地写入一张新表 `sys_sql_audit_log`，并提供管理端分页查询。

产出物：

1. SQL 审计落库表 `sys_sql_audit_log`（DDL）；
2. 审计消息采集 → 批量落库的自定义 Reporter；
3. 审计开关与自动配置类；
4. 管理端查询接口与权限码。

---

## 二、MyBatis-Flex 审计机制

### 2.1 核心类（`com.mybatisflex.core.audit`）

| 类/接口 | 职责 |
| --- | --- |
| `AuditManager` | 审计总入口（静态全局），控制 `auditEnable`、`MessageFactory`、`MessageCollector`、`MessageReporter` |
| `AuditMessage` | 单条审计消息（承载 SQL、参数、耗时、数据源、调用方等） |
| `MessageCollector` | 收集器接口：`collect(AuditMessage)`，逐条接收 |
| `MessageReporter` | 上报器接口：`sendMessages(List<AuditMessage>)`，批量上报 |
| `AbstractMessageCollector` | 收集器抽象基类：内部缓存消息队列，达到阈值/定时触发 `sendMessages` |
| `CountableMessageCollector` | 按**条数**触发的收集器（构造参数 `count, reporter`） |
| `ScheduledMessageCollector` | 按**时间**触发的收集器（构造参数 `periodMillis, reporter`，`release()` 释放） |
| `ConsoleMessageReporter` | 默认上报器：打印到控制台（Web 环境默认即此） |

### 2.2 触发时机

- MyBatis-Flex 内置的审计拦截器会包裹每一条 SQL 执行，调用 `AuditManager.startAudit(...)`；
- 当 `AuditManager.isAuditEnable() == false`（默认关闭）时直接执行，不产生审计消息；
- **开启后对全部 SQL 生效**（含查询）。因此使用方需要在采集层自行做过滤、降噪、防递归。

### 2.3 注解情况

`mybatis-flex-annotation`（1.11.8）中**不存在** `@Audit` 等可标记在 Mapper 方法上、用于排除/纳入审计的注解。也就是说当前版本是「全局审计」，无法按 Mapper 方法精确开关，**必须在采集层按 `stmtId` 或 SQL 文本做忽略/过滤**。这是本方案的一个关键约束。

### 2.4 AuditMessage 关键字段

| getter | 含义 | 落库字段建议 |
| --- | --- | --- |
| `getPlatform()` | 平台标识（admin/member） | `platform` |
| `getModule()` | 模块名 | `module` |
| `getUrl()` | 请求 URL | `url` |
| `getBizId()` | 业务 ID | `biz_id` |
| `getUser()` | 操作人 | `user` |
| `getUserIp()` | 用户 IP | `user_ip` |
| `getHostIp()` | 主机 IP | `host_ip` |
| `getDsName()` | 数据源名 | `ds_name` |
| `getStmtId()` | Mapper 语句全限定 ID（利于防递归/按语句过滤） | `stmt_id` |
| `getQuery()` | 原始 SQL 文本 | `sql` |
| `getQueryParams()` | SQL 参数（`List<Object>`） | `sql_params`（JSON） |
| `getFullSql()` | 完整 SQL（含内联参数） | `full_sql` |
| `getQueryCount()` | 查询返回条数 | `query_count` |
| `getQueryTime()` | 查询时间戳（ms） | `query_time` |
| `getElapsedTime()` | 执行耗时（ms） | `elapsed_time` |
| `getMetas()` | 扩展元信息 `Map<String,Object>` | 可写入 `metas`（JSON） |

---

## 三、总体架构与数据流

```
业务代码 → mapper.xxx()
               │
               ▼
    MyBatis-Flex 审计拦截器（内置）
               │  auditEnable=true 时
               ▼
        AuditManager.startAudit(...)
               │
               ▼
        AuditMessage（单条 SQL 信息）
               │
               ▼
    MessageCollector（CountableMessageCollector，按条数批量）
               │  reach batch-size
               ▼
    MessageReporter（自定义 SqlAuditMessageReporter）
               │
               │  JdbcTemplate.batchUpdate(...)   ← 绕开 MyBatis-Flex 拦截器（防递归）
               ▼
        sys_sql_audit_log 表
```

### 3.1 关键决策

| 决策点 | 选择 | 理由 |
| --- | --- | --- |
| 落库方式 | **优先 `JdbcTemplate` 批量插入** | 走 MyBatis-Flex 会再次触发审计拦截器，导致无限递归；JdbcTemplate 为原生 JDBC，天然绕开。若坚持用 Mapper，则必须结合「按 stmtId 忽略审计表自身语句」的兜底 |
| 采集策略 | `CountableMessageCollector`（如每 50 条触发） | 控制单事务批量大小，兼顾实时性与写入量 |
| 异步写入 | 独立单线程池 + 有界队列 + 丢弃告警 | SQL 审计不能拖垮/阻塞业务主流程（对齐 `OperateLogService` 现有模式） |
| 业务上下文补全 | 采集时从 Sa-Token/`LoginUserContextHolder` 补 `user`、`platform` | `AuditMessage` 默认不带业务用户信息，需在收集层补（参考 `OperateLogService.fillOperator`） |

---

## 四、数据库设计

### 4.1 表结构 DDL（`sys_sql_audit_log`）

> 落库文件 `sql/06_sql_audit_log.sql`（新增，随既有 SQL 文件顺序编排）。

```sql
-- ============================================================
-- SQL 审计日志表 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 写入方：MyBatis-Flex AuditManager → 自定义 SqlAuditMessageReporter（JdbcTemplate 批量）
-- 说明：日志表只增不改，保留 create_by/create_time 以复用全局填充；update_* / is_delete 保留但通常不更新
-- ============================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `sys_sql_audit_log`;
CREATE TABLE `sys_sql_audit_log` (
    `id`            BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `trace_id`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    `platform`      VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '平台：admin-管理端 member-会员端',
    `module`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所属模块',
    `url`           VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '请求URL',
    `biz_id`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '业务ID（可选）',
    `user`          VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '操作人（用户ID/用户名）',
    `user_ip`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '用户IP',
    `host_ip`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '主机IP',
    `ds_name`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据源名称',
    `stmt_id`       VARCHAR(256)  NOT NULL DEFAULT '' COMMENT 'Mapper 语句全限定ID（com.xxx.Mapper.method）',
    `sql`           TEXT          NOT NULL COMMENT '原始SQL文本，超长截断',
    `sql_params`    TEXT          NOT NULL COMMENT 'SQL参数（JSON），超长截断',
    `full_sql`      TEXT          NOT NULL COMMENT '完整SQL（含内联参数），超长截断',
    `query_count`   INT           NOT NULL DEFAULT 0  COMMENT '查询返回条数',
    `query_time`    DATETIME          NULL COMMENT '查询发生时间（毫秒时间戳转）',
    `elapsed_time`  BIGINT        NOT NULL DEFAULT 0  COMMENT '执行耗时（毫秒）',
    `metas`         TEXT          NOT NULL COMMENT '扩展元信息（JSON），超长截断',
    `create_by`     BIGINT            NULL COMMENT '创建人ID',
    `create_time`   DATETIME          NULL COMMENT '创建时间',
    `update_by`     BIGINT            NULL COMMENT '更新人ID（日志表保留，不更新）',
    `update_time`   DATETIME          NULL COMMENT '更新时间（日志表保留，不更新）',
    `is_delete`     TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_platform_user` (`platform`, `user`),
    KEY `idx_stmt_id` (`stmt_id`),
    KEY `idx_ds_name` (`ds_name`),
    KEY `idx_elapsed_time` (`elapsed_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SQL审计日志表';
```

> `sql` / `full_sql` 可能超表长度，落库前必须截断（见 §10 性能与安全）。`query_time` 上游为毫秒时间戳，落库时转为 `DATETIME`。

---

## 五、实体 / Mapper / Service（示意，待实现）

### 5.1 实体 `SysSqlAuditLog`（`zhush-module-system/.../entity`）

```java
@Table("sys_sql_audit_log")
public class SysSqlAuditLog extends BaseEntity implements Serializable {
    private String traceId;      // 链路追踪ID
    private String platform;     // admin / member
    private String module;       // 所属模块
    private String url;          // 请求URL
    private String bizId;        // 业务ID
    private String user;         // 操作人
    private String userIp;       // 用户IP
    private String hostIp;       // 主机IP
    private String dsName;       // 数据源名称
    private String stmtId;       // Mapper 语句ID
    private String sql;          // 原始SQL
    private String sqlParams;    // 参数JSON
    private String fullSql;      // 完整SQL
    private Integer queryCount;  // 查询返回条数
    private LocalDateTime queryTime; // 查询发生时间
    private Long elapsedTime;    // 执行耗时(ms)
    private String metas;        // 扩展JSON
}
```

> 说明：
> - 继承 `BaseEntity` 复用雪花主键与全局填充（建表语句已含 `create_by/create_time/update_*/is_delete`）。
> - **注意 MyBatis-Flex 非空约束**：字段为空时 `insert()` 会显式插入 `NULL`，因此所有 `NOT NULL DEFAULT ''` 列的 Java 字段应赋默认 `""`（参考既有教训），落库路径统一用 `insertSelective`。

### 5.2 Mapper `SysSqlAuditLogMapper`

```java
@Mapper
public interface SysSqlAuditLogMapper extends BaseMapperX<SysSqlAuditLog> {
}
```

### 5.3 Service `SqlAuditLogService`

职责：
- 管理端分页查询（复用 `QueryWrapperX`，参照 `OperateLogService.getOperateLogPage`）；
- 详情；
- （可选）按时间清理过期记录。

```java
public PageResult<SqlAuditLogVO> getSqlAuditPage(SqlAuditLogQuery query) {
    QueryWrapperX qw = QueryWrapperX.create()
        .eq(SysSqlAuditLog::getPlatform, query.getPlatform())
        .like(SysSqlAuditLog::getUser, query.getUser())
        .like(SysSqlAuditLog::getStmtId, query.getStmtId())
        .ge(SysSqlAuditLog::getQueryTime, query.getStartTime())
        .le(SysSqlAuditLog::getQueryTime, query.getEndTime())
        .orderBy("id", false);
    Page<SysSqlAuditLog> page = sqlAuditLogMapper.selectPageX(query, qw);
    return PageResult.of(converter.convert(page.getRecords(), SqlAuditLogVO.class), page.getTotalRow());
}
```

---

## 六、审计落库 Reporter 设计

### 6.1 `SqlAuditMessageReporter`（实现 `MessageReporter`）

> 由自定义 `MessageCollector` 在满足批次数后调用 `sendMessages(List<AuditMessage>)`。

```java
@Slf4j
public class SqlAuditMessageReporter implements MessageReporter {

    private final JdbcTemplate jdbcTemplate;
    private final SqlAuditProperties properties; // 配置：开关、截断长度、忽略表等
    private final ExecutorService executor;      // 单线程 + 有界队列，见 §6.3

    @Override
    public void sendMessages(List<AuditMessage> messages) {
        // 过滤：忽略审计表自身语句（防递归兜底，若用 JdbcTemplate 可省略）
        List<AuditMessage> filtered = messages.stream()
            .filter(m -> !properties.getIgnoreStmtIds().contains(m.getStmtId()))
            .toList();
        if (filtered.isEmpty()) return;
        executor.execute(() -> batchInsert(filtered));
    }

    private void batchInsert(List<AuditMessage> messages) {
        // 使用 INSERT 语句，字段与表一一对应；sql/sql_params/full_sql/metas 做长度截断；
        // query_time 将毫秒时间戳转为 java.sql.Timestamp。
        // 用 JdbcTemplate.batchUpdate(...) 落库，绕开 MyBatis-Flex 拦截器（防递归）。
    }
}
```

### 6.2 业务上下文补全（关键）

`AuditMessage` 由拦截器填充的是全局 SQL 信息，不包含当前登录用户/平台。在采集层（`MessageCollector`，或生产 `MessageFactory`）中，从 `StpAdminUtil` / `StpMemberUtil` / `LoginUserContextHolder` 补 `user`、`platform`（参考 `OperateLogService.fillOperator`）：

```java
message.setUser(String.valueOf(StpAdminUtil.getLoginIdDefaultNull() != null
        ? StpAdminUtil.getLoginIdDefaultNull()
        : StpMemberUtil.getLoginIdDefaultNull()));
message.setPlatform(...); // admin / member / anonymous
message.setTraceId(...);  // 若有 SkyWalking/自定义上下文可补
```

> 注意：采集发生在 SQL 执行前（`startAudit` 包裹），此时请求线程的 Sa-Token 上下文可用；异步落库线程则不再可用，因此需在采集/构建阶段完成补全，而不是落库阶段。

### 6.3 异步线程池（对齐 `OperateLogService`）

```java
private final ExecutorService auditExecutor = new ThreadPoolExecutor(
        1, 1, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(8192),
        r -> {
            Thread t = new Thread(r, "sql-audit-writer");
            t.setDaemon(true);
            return t;
        },
        (r, ex) -> log.warn("[SqlAudit] 审计队列已满，丢弃一条SQL审计记录"));

@PreDestroy
public void shutdown() { /* 参照 OperateLogService.shutdown() */ }
```

---

## 七、自动配置类

放置位置：建议 `zhush-framework/zhush-spring-boot-starter-mybatis`（与 `GlobalFlexAutoConfiguration` 同层，方便统一管理全局 SQL 能力）。

```java
@AutoConfiguration(after = ZhushDataSourceAutoConfiguration.class)
@EnableConfigurationProperties(SqlAuditProperties.class)
public class SqlAuditAutoConfiguration {

    @Bean
    public SqlAuditMessageReporter sqlAuditMessageReporter(
            JdbcTemplate jdbcTemplate, SqlAuditProperties properties) {
        return new SqlAuditMessageReporter(jdbcTemplate, properties);
    }

    @Bean
    public SqlAuditInitializer sqlAuditInitializer(
            SqlAuditProperties properties, SqlAuditMessageReporter reporter) {
        return new SqlAuditInitializer(properties, reporter);
    }
}
```

初始化逻辑（`ApplicationRunner` / `InitializingBean` 均可）：

```java
public void init() {
    if (!properties.isEnabled()) {
        AuditManager.setAuditEnable(false);
        return;
    }
    // 按条数批量触发
    MessageReporter reporter = new CountingReporterAdapter(sqlAuditMessageReporter); // 或直接传给收集器
    int batchSize = properties.getBatchSize();
    AuditManager.setMessageCollector(new CountableMessageCollector(batchSize, reporter));
    AuditManager.setAuditEnable(true);
}
```

> 需要调研点（实现阶段确认）：`CountableMessageCollector` 的 `collect` 内部是否已具备"达到阈值即 `doSendMessages`"，还是需要在子类里主动在每次 `collect` 后判满并发送。若默认只缓存不触发，则需自定义一个 `AbstractMessageCollector` 子类，在 `collect` 内判断 `getMessages().size() >= batchSize` 后调用 `doSendMessages()` 并清空。

---

## 八、配置项（`application.yml`）

```yaml
zhush:
  sql-audit:
    enabled: false          # 总开关（默认关闭，避免全量审计带来性能开销）
    batch-size: 50          # 收集多少条触发一次落库
    sample-rate: 100        # 采样率(%): 100=全量, 10=每10条录1条; 降噪用
    sql-max-length: 4000    # 原始SQL截断长度
    full-sql-max-length: 8000 # 完整SQL截断长度
    slow-sql-threshold: 500 # 慢SQL阈值(ms)，低于该值可选择性跳过落库
    only-write: false       # true=仅审计 INSERT/UPDATE/DELETE，跳过 SELECT
    ignore-tables:          # 忽略的表名（如审计表自身）
      - sys_sql_audit_log
    ignore-stmt-ids:        # 忽略的 Mapper 语句（防递归兜底）
      - com.zhush.system.mapper.SysSqlAuditLogMapper.insert
```

配合 `@ConfigurationProperties("zhush.sql-audit")` 映射到 `SqlAuditProperties`。

---

## 九、查询接口与菜单权限

- 管理端接口（`zhush-module-system`）：

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/admin/system/sql-audit/page` | `system:sql-audit:query` |
| GET | `/admin/system/sql-audit/{id}` | `system:sql-audit:query` |
| DELETE | `/admin/system/sql-audit/{id}` | `system:sql-audit:delete` |

- 参照现有 `AdminOperateLogController` 的 `@SaCheckPermission`、`@OperateLog`、分页写法。
- 菜单：`sys_menu` 插入「SQL审计」菜单，权限码 `system:sql-audit:query`（ID 遵循项目菜单 ID 分段规则）。

---

## 十、关键设计要点与风险

### 10.1 防递归（最重要）

- 首选：落库用 `JdbcTemplate`（原生 JDBC），完全绕开 MyBatis-Flex 审计拦截器，**无递归**。
- 兜底：若改用 Mapper 落库，则必须在 `SqlAuditProperties.ignore-stmt-ids` 里放审计表自身的 Mapper 语句（如 `SysSqlAuditLogMapper.insert` / `insertSelective`），`MessageReporter` 落库前过滤掉这些 `stmtId`。

### 10.2 性能

- 全量 SQL 审计量极大（含查询、高频接口）。默认 `enabled=false`，按需开启；
- 提供「仅写操作 / 采样率 / 慢 SQL 阈值 / 忽略表」降噪；
- 异步单线程 + 有界队列 + 丢弃告警，确保不会反压业务线程。

### 10.3 敏感信息脱敏

- SQL 参数可能含密码、Token、手机号等。落库前对 `sql_params` / `full_sql` 做脱敏（如 `password=***`），避免审计表变"泄露源"。
- 建议默认 `full_sql` 可选择不落库，仅落 `sql` + 参数摘要。

### 10.4 长度截断

- `sql` / `sql_params` / `full_sql` / `metas` 超长需在 `SqlAuditMessageReporter` 中截断，防止 `Data too long` 落库失败。

### 10.5 时序与数据量

- 日志只增不改，长期运行数据量持续增长：提供按 `create_time` 的定期清理（建议以天为周期，保留 N 天，`is_delete` 或物理删除）。
- 索引已建 `create_time` / `stmt_id`，支撑按时间清理与按语句检索。

### 10.6 与既有操作日志的关系

- `sys_operate_log`（HTTP 业务日志）与 `sys_sql_audit_log`（SQL 日志）互补，不互相替代；业务日志适合追溯"人做了什么"，SQL 日志适合排查"底层 SQL 性能/异常"，两者可凭 `trace_id` 关联。

---

## 十一、实现清单（待开发）

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| SQL | `sql/06_sql_audit_log.sql` | 建表 DDL |
| 实体 | `zhush-module-system/.../entity/SysSqlAuditLog.java` | 审计实体 |
| Mapper | `zhush-module-system/.../mapper/SysSqlAuditLogMapper.java` | BaseMapperX |
| Service | `zhush-module-system/.../service/SqlAuditLogService.java` | 分页/详情 |
| Controller | `zhush-module-system/.../controller/admin/AdminSqlAuditLogController.java` | 查询接口 |
| Properties | `zhush-framework/.../starter-mybatis/config/SqlAuditProperties.java` | `zhush.sql-audit.*` 配置 |
| Reporter | `zhush-framework/.../starter-mybatis/audit/SqlAuditMessageReporter.java` | `MessageReporter` 落库实现 |
| Config | `zhush-framework/.../starter-mybatis/config/SqlAuditAutoConfiguration.java` | 注册审计开关与收集器 |
| 菜单 | `sql/` 菜单 INSERT | `system:sql-audit:query` |

---

## 十二、落地前需确认的 3 个问题

1. **`CountableMessageCollector` 触发行为**：确认其是否在达到条数后自动 `doSendMessages()`，还是仅缓存（若仅缓存则自定义收集器子类）。实现阶段以 `javap` 反编译/文档为准。
2. **落库通道**：选用 `JdbcTemplate`（推荐，防递归）还是 `SysSqlAuditLogMapper`（需配 `ignore-stmt-ids`）。
3. **审计范围**：默认全量审计会覆盖全部模块 SQL，生产建议仅开启「写操作」+ 采样，或限定在需要排查的短窗口内开启。
