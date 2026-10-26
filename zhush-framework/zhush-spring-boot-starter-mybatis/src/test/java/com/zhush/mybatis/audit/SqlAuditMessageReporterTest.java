package com.zhush.mybatis.audit;

import com.mybatisflex.core.audit.AuditMessage;
import com.zhush.mybatis.config.SqlAuditProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQL 审计落库语句单测。
 *
 * <p>落库刻意走原生 {@link JdbcTemplate} 以绕开 MyBatis-Flex 审计拦截器（防递归），代价是不再享受
 * Flex 方言的标识符反引号包装与列默认值兜底，因此语句本身必须自带 MySQL 语法安全。
 */
class SqlAuditMessageReporterTest {

    /** INSERT 语句的列清单片段 */
    private static final Pattern COLUMN_SECTION =
            Pattern.compile("\\(([^()]*)\\)\\s*VALUES", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** DDL 中 NOT NULL DEFAULT '' 的列：无登录上下文时写 null 会触发 1048 */
    private static final List<String> NOT_NULL_COLUMNS = List.of(
            "trace_id", "platform", "module", "url", "biz_id", "user", "user_ip", "host_ip",
            "ds_name", "stmt_id", "sql", "sql_params", "full_sql", "metas");

    /** sys_sql_audit_log 的落库列数（与 sql/19_sql_audit_log.sql 对齐） */
    private static final int COLUMN_COUNT = 23;

    @Test
    void everyIdentifierShouldBeBackQuoted() {
        Captured captured = capture();

        assertThat(captured.columns()).isNotEmpty().hasSize(COLUMN_COUNT);
        assertThat(captured.columns()).allSatisfy(column ->
                assertThat(column).startsWith("`").endsWith("`"));
        // sql / user / module 等均为 MySQL 关键字，裸写会报 1064
        assertThat(captured.columns()).contains("`sql`", "`user`", "`module`");
    }

    @Test
    void notNullColumnsShouldNeverReceiveNull() {
        Map<String, Object> row = capture().rowByColumn();

        NOT_NULL_COLUMNS.forEach(column ->
                assertThat(row.get(column)).as("列 %s 为 NOT NULL，不允许写入 null", column).isNotNull());
        assertThat(row.get("platform")).isEqualTo("");
        assertThat(row.get("url")).isEqualTo("");
    }

    @Test
    void placeholderCountShouldMatchColumns() {
        Captured captured = capture();

        assertThat(captured.placeholders()).isEqualTo(captured.columns().size());
        assertThat(captured.rowByColumn()).hasSize(COLUMN_COUNT);
        assertThat(captured.rowByColumn().get("is_delete")).isEqualTo(0);
        assertThat(captured.rowByColumn().get("create_time")).isNotNull();
    }

    /**
     * 后台线程（定时任务、异步事件）没有请求上下文，采集层取不到所属模块，只能由 stmtId 的 mapper 段反推
     */
    @Test
    void moduleShouldFallBackToMapperPackageWhenRequestContextMissing() {
        AuditMessage message = mock(AuditMessage.class);
        when(message.getStmtId()).thenReturn("com.zhush.ai.mapper.AiGenerationTaskMapper.selectListByQuery");

        assertThat(capture(message).rowByColumn().get("module")).isEqualTo("ai");
    }

    // ================ 内部 ================

    /** 一次落库调用捕获到的语句与行数据 */
    private record Captured(List<String> columns, int placeholders, Map<String, Object> rowByColumn) {
    }

    /**
     * 用一条不带任何业务上下文的审计消息触发落库，捕获语句与行参数
     */
    private Captured capture() {
        return capture(mock(AuditMessage.class));
    }

    /**
     * 触发一次落库并捕获语句与行参数
     */
    @SuppressWarnings("unchecked")
    private Captured capture(AuditMessage message) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        new SqlAuditMessageReporter(jdbcTemplate, new SqlAuditProperties())
                .sendMessages(List.of(message));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), argsCaptor.capture());

        String sql = sqlCaptor.getValue();
        List<String> columns = columnsOf(sql);
        Object[] args = argsCaptor.getValue().get(0);
        assertThat(args).as("行参数与列数不一致: %s", sql).hasSize(columns.size());

        Map<String, Object> rowByColumn = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            rowByColumn.put(columns.get(i).replace("`", ""), args[i]);
        }
        return new Captured(columns, countPlaceholders(sql), rowByColumn);
    }

    private static List<String> columnsOf(String sql) {
        Matcher matcher = COLUMN_SECTION.matcher(sql);
        if (!matcher.find()) {
            throw new AssertionError("未解析到 INSERT 列清单: " + sql);
        }
        List<String> columns = new ArrayList<>();
        for (String column : matcher.group(1).split(",")) {
            String trimmed = column.trim();
            if (!trimmed.isEmpty()) {
                columns.add(trimmed);
            }
        }
        return columns;
    }

    private static int countPlaceholders(String sql) {
        Matcher matcher = Pattern.compile("\\?")
                .matcher(sql.substring(sql.toUpperCase().indexOf("VALUES")));
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
