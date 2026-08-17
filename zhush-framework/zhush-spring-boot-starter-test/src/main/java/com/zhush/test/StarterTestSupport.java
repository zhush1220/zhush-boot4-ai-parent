package com.zhush.test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * 装配冒烟测试基类
 *
 * <p>面向各 starter 的「开关开/关」装配测试：子类继承后通过
 * {@link #runner(Class...)} / {@link #webRunner(Class...)} 创建独立测试上下文
 * （不启动完整应用，不依赖外部中间件），配合
 * {@link #assertSingleBean} / {@link #assertNoBean} 断言装配结果。
 *
 * <p>示例：
 * <pre>{@code
 * class DemoAutoConfigurationTest extends StarterTestSupport {
 *     @Test
 *     void disabled() {
 *         assertNoBean(runner(DemoAutoConfiguration.class).withPropertyValues("zhush.demo.enabled=false"),
 *                 DemoUtils.class);
 *     }
 *     @Test
 *     void enabled() {
 *         assertSingleBean(runner(DemoAutoConfiguration.class).withPropertyValues("zhush.demo.enabled=true"),
 *                 DemoUtils.class);
 *     }
 * }
 * }</pre>
 *
 * @author zhush
 */
public abstract class StarterTestSupport {

    /**
     * 创建普通 ApplicationContext 测试运行器（自动装配类入参）
     */
    protected ApplicationContextRunner runner(Class<?>... autoConfigurations) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(autoConfigurations));
    }

    /**
     * 创建 Servlet Web 环境测试运行器（@ConditionalOnWebApplication 场景）
     */
    protected WebApplicationContextRunner webRunner(Class<?>... autoConfigurations) {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(autoConfigurations));
    }

    /**
     * 断言上下文中存在且仅存在一个指定类型的 Bean
     */
    protected void assertSingleBean(ApplicationContextRunner runner, Class<?> type) {
        runner.run(context -> assertBeanCount(context.getBeanNamesForType(type), type, 1));
    }

    /**
     * 断言上下文中存在且仅存在一个指定类型的 Bean（Servlet Web 环境）
     */
    protected void assertSingleBean(WebApplicationContextRunner runner, Class<?> type) {
        runner.run(context -> assertBeanCount(context.getBeanNamesForType(type), type, 1));
    }

    /**
     * 断言上下文中不存在指定类型的 Bean
     */
    protected void assertNoBean(ApplicationContextRunner runner, Class<?> type) {
        runner.run(context -> assertBeanCount(context.getBeanNamesForType(type), type, 0));
    }

    /**
     * 断言上下文中不存在指定类型的 Bean（Servlet Web 环境）
     */
    protected void assertNoBean(WebApplicationContextRunner runner, Class<?> type) {
        runner.run(context -> assertBeanCount(context.getBeanNamesForType(type), type, 0));
    }

    private void assertBeanCount(String[] beanNames, Class<?> type, int expected) {
        if (beanNames.length != expected) {
            throw new AssertionError("期望 " + type.getName() + " Bean 数量为 " + expected + "，实际 " + beanNames.length);
        }
    }
}
