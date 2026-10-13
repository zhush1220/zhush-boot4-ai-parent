package com.zhush.data.permission.config;

import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.DataScopeType;
import com.zhush.data.permission.core.PermissionChecker;
import com.zhush.data.permission.core.aop.DataPermissionAspect;
import com.zhush.data.permission.core.dialect.DataPermissionDialect;
import com.zhush.data.permission.core.field.FieldPermissionResponseAdvice;
import com.zhush.test.StarterTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限 starter 装配冒烟测试（Servlet 环境，多级开关开/关）
 * <p>
 * 同时校验业务侧未实现 SPI 时的默认行为：不过滤数据范围、字段全部放行。
 *
 * @author zhush
 */
class DataPermissionAutoConfigurationTest extends StarterTestSupport {

    @Test
    void disabledByDefault() {
        WebApplicationContextRunner runner = webRunner(DataPermissionAutoConfiguration.class);
        assertNoBean(runner, DataPermissionAspect.class);
        assertNoBean(runner, DataPermissionDialect.class);
        assertNoBean(runner, FieldPermissionResponseAdvice.class);
    }

    @Test
    void disabledExplicitly() {
        assertNoBean(webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=false"), FieldPermissionResponseAdvice.class);
    }

    @Test
    void enabledRegistersRowAndFieldBeans() {
        WebApplicationContextRunner runner = webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=true");
        assertSingleBean(runner, DataPermissionAspect.class);
        assertSingleBean(runner, DataPermissionDialect.class);
        assertSingleBean(runner, FieldPermissionResponseAdvice.class);
    }

    @Test
    void rowSwitchOffKeepsFieldPermission() {
        WebApplicationContextRunner runner = webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=true", "zhush.data-permission.row-enabled=false");
        assertNoBean(runner, DataPermissionAspect.class);
        assertNoBean(runner, DataPermissionDialect.class);
        assertSingleBean(runner, FieldPermissionResponseAdvice.class);
    }

    @Test
    void fieldSwitchOffKeepsRowPermission() {
        WebApplicationContextRunner runner = webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=true", "zhush.data-permission.field-enabled=false");
        assertSingleBean(runner, DataPermissionAspect.class);
        assertNoBean(runner, FieldPermissionResponseAdvice.class);
    }

    @Test
    void defaultSpiDoesNotFilterOrRestrict() {
        webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=true")
                .run(context -> {
                    DataScope scope = context.getBean(DataScopeProvider.class).getDataScope();
                    // 默认数据范围为“全部”，默认字段校验器全部放行
                    assertThat(scope.getType()).isEqualTo(DataScopeType.ALL);
                    assertThat(context.getBean(PermissionChecker.class).hasPermission("any:permission")).isTrue();
                });
    }

    @Test
    void businessSpiOverridesDefault() {
        // 业务侧注册自己的 SPI 实现后不再产生默认 Bean（@ConditionalOnMissingBean）
        DataScope selfScope = DataScope.self(9L);
        webRunner(DataPermissionAutoConfiguration.class)
                .withPropertyValues("zhush.data-permission.enabled=true")
                .withBean(DataScopeProvider.class, () -> () -> selfScope)
                .withBean(PermissionChecker.class, () -> permission -> false)
                .run(context -> {
                    assertThat(context.getBean(DataScopeProvider.class).getDataScope().getType())
                            .isEqualTo(DataScopeType.SELF);
                    assertThat(context.getBean(PermissionChecker.class).hasPermission("any:permission")).isFalse();
                });
    }
}
