package com.zhush.mybatis.audit;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.MessageReporter;
import com.zhush.mybatis.config.SqlAuditProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 审计消息上报器
 * <p>
 * 实现 MyBatis-Flex 的 {@link MessageReporter}，由 {@code CountableMessageCollector} 在达到批量阈值后回调，
 * 将一批 {@link AuditMessage} 通过 {@link JdbcTemplate} 批量写入 {@code sys_sql_audit_log}。
 * <p>
 * 选择原生 JDBC（而非 MyBatis-Flex Mapper）落库的核心原因：
 * <ol>
 *     <li>JdbcTemplate 走 JDBC 直连，不会经过 MyBatis-Flex 审计拦截器，天然避免「落库 SQL 再次触发审计」的无限递归；</li>
 *     <li>避免每次落库造成审计死循环或性能损耗。</li>
 * </ol>
 * 另外，该方法运行在收集器启动的独立线程中，不会阻塞业务请求线程。
 *
 * @author zhush
 * @since 2026/9/5
 */
@Slf4j
public class SqlAuditMessageReporter implements MessageReporter {

    /**
     * 写入审计表的 INSERT 语句（23 列占位符）
     */
    private static final String INSERT_SQL = """
            INSERT INTO sys_sql_audit_log
            (id, trace_id, platform, module, url, biz_id, user, user_ip, host_ip,
             ds_name, stmt_id, sql, sql_params, full_sql, query_count, query_time,
             elapsed_time, metas, create_by, create_time, update_by, update_time, is_delete)
            VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /**
     * 敏感参数脱敏正则：匹配 password/token/secret/api_key 等键值对
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)((?:password|pwd|token|secret|access_?key|api_?key)\\s*[:=]\\s*)(['\"]?)([^,'\"\\s]+)");

    private final JdbcTemplate jdbcTemplate;
    private final SqlAuditProperties properties;

    public SqlAuditMessageReporter(JdbcTemplate jdbcTemplate, SqlAuditProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void sendMessages(List<AuditMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<AuditMessage> audited = messages.stream().filter(this::isAudited).toList();
        if (audited.isEmpty()) {
            return;
        }
        try {
            List<Object[]> batchArgs = audited.stream().map(this::toRow).toList();
            jdbcTemplate.batchUpdate(INSERT_SQL, batchArgs);
        } catch (Exception e) {
            log.error("[SqlAudit] SQL 审计落库失败, count={}", audited.size(), e);
        }
    }

    /**
     * 判断单条审计消息是否需要落库（降噪 + 防递归 + 采样）
     */
    private boolean isAudited(AuditMessage message) {
        String sql = StrUtil.trim(StrUtil.nullToEmpty(message.getQuery()));
        String lowerSql = sql.toLowerCase(Locale.ROOT);

        // 过滤：忽略指定 Mapper 语句（防递归兜底）
        if (StrUtil.isNotBlank(message.getStmtId()) && inIgnoreList(message.getStmtId(),
                properties.getIgnoreStmtIds())) {
            return false;
        }
        // 过滤：忽略指定表
        if (containsAny(lowerSql, properties.getIgnoreTables())) {
            return false;
        }
        // 过滤：仅写操作
        if (properties.isOnlyWrite() && !isWrite(lowerSql)) {
            return false;
        }
        // 过滤：慢 SQL（阈值大于 0 时仅记录慢 SQL）
        if (properties.getSlowSqlThreshold() > 0 && message.getElapsedTime() < properties.getSlowSqlThreshold()) {
            return false;
        }
        // 采样
        if (properties.getSampleRate() < 100
                && ThreadLocalRandom.current().nextInt(100) >= properties.getSampleRate()) {
            return false;
        }
        return true;
    }

    /**
     * 将审计消息转为数据库行
     */
    private Object[] toRow(AuditMessage message) {
        LocalDateTime now = LocalDateTime.now();
        long queryTime = message.getQueryTime();

        return new Object[]{
                IdUtil.getSnowflakeNextId(),
                traceIdFrom(message),
                StrUtil.maxLength(message.getPlatform(), 16),
                StrUtil.maxLength(message.getModule(), 64),
                StrUtil.maxLength(message.getUrl(), 256),
                StrUtil.maxLength(message.getBizId(), 64),
                StrUtil.maxLength(message.getUser(), 64),
                StrUtil.maxLength(message.getUserIp(), 64),
                StrUtil.maxLength(message.getHostIp(), 64),
                StrUtil.maxLength(message.getDsName(), 64),
                StrUtil.maxLength(message.getStmtId(), 256),
                truncate(message.getQuery(), properties.getSqlMaxLength()),
                truncate(maskSensitive(paramsJson(message)), properties.getSqlParamsMaxLength()),
                truncate(maskSensitive(message.getFullSql()), properties.getFullSqlMaxLength()),
                message.getQueryCount(),
                queryTime > 0 ? new Timestamp(queryTime) : null,
                message.getElapsedTime(),
                truncate(metasJson(message.getMetas()), properties.getMetasMaxLength()),
                null,
                now,
                null,
                null,
                0
        };
    }

    /**
     * metas 转为 JSON 字符串（空则返回空串，避免写入 "null"）
     */
    private static String metasJson(Map<String, Object> metas) {
        if (metas == null || metas.isEmpty()) {
            return "";
        }
        return JSONUtil.toJsonStr(metas);
    }

    /**
     * SQL 参数转为 JSON 字符串（空则返回空串，避免写入 "null"）
     */
    private static String paramsJson(AuditMessage message) {
        List<?> params = message.getQueryParams();
        if (params == null || params.isEmpty()) {
            return "";
        }
        return JSONUtil.toJsonStr(params);
    }

    /**
     * 从 metas 提取 traceId
     */
    private String traceIdFrom(AuditMessage message) {
        Map<String, Object> metas = message.getMetas();
        if (metas == null || !metas.containsKey("traceId")) {
            return "";
        }
        return StrUtil.maxLength(String.valueOf(metas.get("traceId")), 64);
    }

    /**
     * 字符串截断（null 转空）
     */
    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return StrUtil.maxLength(value, maxLength);
    }

    /**
     * SQL 是否为写操作
     */
    private static boolean isWrite(String lowerSql) {
        return lowerSql.startsWith("insert") || lowerSql.startsWith("update") || lowerSql.startsWith("delete");
    }

    /**
     * SQL 是否包含任一忽略表名
     */
    private static boolean containsAny(String lowerSql, List<String> lowerTables) {
        if (lowerTables == null || lowerTables.isEmpty()) {
            return false;
        }
        return lowerTables.stream().anyMatch(table -> lowerSql.contains(table.toLowerCase(Locale.ROOT)));
    }

    /**
     * 是否命中忽略列表（全限定语句ID）
     */
    private static boolean inIgnoreList(String stmtId, List<String> ignoreList) {
        if (ignoreList == null || ignoreList.isEmpty()) {
            return false;
        }
        return ignoreList.stream().anyMatch(item -> stmtId.equals(item) || stmtId.startsWith(item));
    }

    /**
     * 对敏感键值进行脱敏
     */
    private static String maskSensitive(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        Matcher matcher = SENSITIVE_PATTERN.matcher(text);
        return matcher.replaceAll("$1$2***");
    }
}
