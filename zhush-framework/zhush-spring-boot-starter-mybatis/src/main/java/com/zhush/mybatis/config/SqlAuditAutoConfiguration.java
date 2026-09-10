package com.zhush.mybatis.config;

import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.audit.CountableMessageCollector;
import com.zhush.mybatis.audit.SqlAuditMessageFactory;
import com.zhush.mybatis.audit.SqlAuditMessageReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MyBatis-Flex SQL 审计自动配置
 * <p>
 * 在 {@code zhush.sql-audit.enabled=true} 时启动 SQL 审计：
 * <ol>
 *     <li>设置自定义 {@link SqlAuditMessageFactory}：在创建审计消息时补全操作人/平台/链路追踪ID；</li>
 *     <li>设置 {@link CountableMessageCollector}：达到批量阈值后异步触发落库；</li>
 *     <li>开启 {@link AuditManager} 审计总开关。</li>
 * </ol>
 * 落库通过 {@link SqlAuditMessageReporter}（原生 JdbcTemplate）完成，绕开 MyBatis-Flex 拦截器避免递归。
 *
 * @author zhush
 * @since 2026/9/5
 */
@Slf4j
@AutoConfiguration(after = ZhushDataSourceAutoConfiguration.class)
@EnableConfigurationProperties(SqlAuditProperties.class)
public class SqlAuditAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "zhush.sql-audit", name = "enabled", havingValue = "true")
    public SqlAuditInitializer sqlAuditInitializer(SqlAuditProperties properties, JdbcTemplate jdbcTemplate) {
        return new SqlAuditInitializer(properties, jdbcTemplate);
    }

    /**
     * 审计初始化器：容器启动后装配 MyBatis-Flex 审计管理器
     */
    @Slf4j
    public static class SqlAuditInitializer implements InitializingBean {

        private final SqlAuditProperties properties;
        private final JdbcTemplate jdbcTemplate;

        public SqlAuditInitializer(SqlAuditProperties properties, JdbcTemplate jdbcTemplate) {
            this.properties = properties;
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public void afterPropertiesSet() {
            SqlAuditMessageReporter reporter = new SqlAuditMessageReporter(jdbcTemplate, properties);
            AuditManager.setMessageFactory(new SqlAuditMessageFactory());
            AuditManager.setMessageCollector(new CountableMessageCollector(properties.getBatchSize(), reporter));
            AuditManager.setAuditEnable(true);
            log.info("[SqlAudit] MyBatis-Flex SQL 审计已启用, batchSize={}, sampleRate={}%, onlyWrite={}",
                    properties.getBatchSize(), properties.getSampleRate(), properties.isOnlyWrite());
        }
    }
}
