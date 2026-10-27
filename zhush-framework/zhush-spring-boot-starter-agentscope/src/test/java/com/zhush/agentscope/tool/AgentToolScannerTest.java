package com.zhush.agentscope.tool;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具扫描器对代理 Bean 的兼容性测试。
 *
 * <p>工具类一旦被 {@code @Transactional}、{@code @Async} 或切面命中即为 CGLIB 代理，代理子类覆写的方法
 * 不携带 {@link AgentTool} 注解；按代理类取注解会静默漏注册。
 *
 * @author zhush
 */
class AgentToolScannerTest {

    @Test
    void registersToolMethodOnCglibProxiedBean() {
        ProxyFactory proxyFactory = new ProxyFactory(new DemoTools());
        proxyFactory.setProxyTargetClass(true);
        DemoTools proxy = (DemoTools) proxyFactory.getProxy();

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(DemoTools.class, () -> proxy);
            context.refresh();

            DefaultToolkitRegistry registry = new DefaultToolkitRegistry();
            AgentToolScanner scanner = new AgentToolScanner(registry);
            scanner.setApplicationContext(context);
            scanner.scan();

            List<String> names = registry.all().stream().map(RegisteredTool::getName).toList();
            assertThat(names).contains("demoEcho");
        }
    }

    public static class DemoTools {

        @AgentTool(name = "demoEcho", description = "测试工具：原样返回入参")
        public String echo(String text) {
            return text;
        }
    }
}
