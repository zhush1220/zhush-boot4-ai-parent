package com.zhush.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis-Flex SQL 审计配置属性
 * <p>
 * 通过 {@code zhush.sql-audit.*} 前缀在 application.yaml 中配置：
 * <pre>{@code
 * zhush:
 *   sql-audit:
 *     enabled: true            # 总开关（默认关闭，避免全量审计带来的性能开销）
 *     batch-size: 50           # 收集多少条触发一次落库
 *     sample-rate: 100         # 采样率(%)，100=全量
 *     sql-max-length: 4000     # 原始SQL截断长度
 *     full-sql-max-length: 8000
 *     sql-params-max-length: 2000
 *     metas-max-length: 2000
 *     slow-sql-threshold: 0    # 慢SQL阈值(ms)，0=不启用（不按耗时过滤）
 *     only-write: false        # true=仅审计 INSERT/UPDATE/DELETE
 *     ignore-tables: []        # 忽略的表名
 *     ignore-stmt-ids: []      # 忽略的 Mapper 语句（防递归兜底）
 * }</pre>
 *
 * @author zhush
 * @since 2026/9/5
 */
@Data
@ConfigurationProperties(prefix = "zhush.sql-audit")
public class SqlAuditProperties {

    /**
     * 是否启用 SQL 审计（全局开关）
     */
    private boolean enabled = false;

    /**
     * 收集多少条审计消息触发一次落库
     */
    private int batchSize = 50;

    /**
     * 采样率（%），100 表示全量采集，10 表示每 10 条录 1 条
     */
    private int sampleRate = 100;

    /**
     * 原始 SQL 截断长度
     */
    private int sqlMaxLength = 4000;

    /**
     * 完整 SQL（含参数）截断长度
     */
    private int fullSqlMaxLength = 8000;

    /**
     * SQL 参数 JSON 截断长度
     */
    private int sqlParamsMaxLength = 2000;

    /**
     * 扩展元信息 JSON 截断长度
     */
    private int metasMaxLength = 2000;

    /**
     * 慢 SQL 阈值（毫秒），大于 0 时仅记录耗时不低于该值的 SQL；0 表示不按耗时过滤
     */
    private long slowSqlThreshold = 0;

    /**
     * 是否仅审计写操作（INSERT/UPDATE/DELETE），true 时跳过 SELECT
     */
    private boolean onlyWrite = false;

    /**
     * 忽略的表名（SQL 中包含这些表名时不落库）
     */
    private List<String> ignoreTables = new ArrayList<>();

    /**
     * 忽略的 Mapper 语句全限定 ID（防递归兜底）
     */
    private List<String> ignoreStmtIds = new ArrayList<>();
}
