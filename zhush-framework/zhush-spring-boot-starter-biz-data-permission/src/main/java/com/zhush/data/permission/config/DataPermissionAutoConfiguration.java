package com.zhush.data.permission.config;

import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.dialect.DialectFactory;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.PermissionChecker;
import com.zhush.data.permission.core.aop.DataPermissionAspect;
import com.zhush.data.permission.core.dialect.DataPermissionDialect;
import com.zhush.data.permission.core.field.FieldPermissionResponseAdvice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据权限自动配置
 * <p>
 * 启停控制（多级开关，默认整体关闭，对未启用的环境零侵入）：
 * <ul>
 *     <li>总开关：{@code zhush.data-permission.enabled=true} 开启</li>
 *     <li>行级子开关：{@code zhush.data-permission.row-enabled}（默认 true）</li>
 *     <li>字段级子开关：{@code zhush.data-permission.field-enabled}（默认 true）</li>
 *     <li>方法级：{@code @DataPermission(enabled = false)} 关闭单个方法</li>
 *     <li>代码级：{@code DataPermissionHelper.ignore(...)} 临时跳过</li>
 * </ul>
 * 业务侧接入：实现 {@link DataScopeProvider}（行级数据范围）与
 * {@link PermissionChecker}（字段权限判定）并注册为 Bean 即可替换默认实现。
 *
 * @author zhush
 */
@AutoConfiguration
@EnableConfigurationProperties(DataPermissionProperties.class)
@ConditionalOnProperty(prefix = DataPermissionProperties.PREFIX, name = "enabled", havingValue = "true")
public class DataPermissionAutoConfiguration {

    /**
     * 默认数据范围提供者：全部数据（业务侧未实现 SPI 时不做行级过滤）
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeProvider dataScopeProvider() {
        return DataScope::all;
    }

    /**
     * 默认字段权限校验器：全部放行（业务侧未实现 SPI 时字段级权限不生效）
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return permission -> true;
    }

    /**
     * 行级权限：@DataPermission 切面 + MyBatis-Flex 数据权限方言
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.mybatisflex.core.dialect.DialectFactory")
    @ConditionalOnProperty(prefix = DataPermissionProperties.PREFIX, name = "row-enabled",
            havingValue = "true", matchIfMissing = true)
    public static class RowPermissionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public DataPermissionAspect dataPermissionAspect() {
            return new DataPermissionAspect();
        }

        @Bean
        @ConditionalOnMissingBean
        public DataPermissionDialect dataPermissionDialect(ObjectProvider<DataScopeProvider> dataScopeProviders) {
            DataPermissionDialect dialect = new DataPermissionDialect(dataScopeProviders);
            // 覆盖 MySQL 默认方言，使 prepareAuth 数据权限钩子生效
            DialectFactory.registerDialect(DbType.MYSQL, dialect);
            return dialect;
        }
    }

    /**
     * 字段级权限：响应体字段置空/脱敏过滤器（仅 Servlet Web 环境生效）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = DataPermissionProperties.PREFIX, name = "field-enabled",
            havingValue = "true", matchIfMissing = true)
    public static class FieldPermissionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public FieldPermissionResponseAdvice fieldPermissionResponseAdvice(PermissionChecker permissionChecker) {
            return new FieldPermissionResponseAdvice(permissionChecker);
        }
    }
}
