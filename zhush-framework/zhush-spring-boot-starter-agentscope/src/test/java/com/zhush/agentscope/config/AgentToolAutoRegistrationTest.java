package com.zhush.agentscope.config;

import com.zhush.agentscope.agent.AgentDefinitionProvider;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.tool.RegisteredTool;
import com.zhush.agentscope.tool.ToolkitRegistry;
import com.zhush.test.StarterTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @AgentTool} 工具的启动期自动注册装配测试。
 *
 * <p>缺陷背景：扫描器曾由 {@code @Bean @ConditionalOnMissingBean} 的
 * {@link org.springframework.beans.factory.SmartInitializingSingleton} 触发，条件按返回类型匹配到
 * 容器基础设施 Bean 后恒不成立，扫描从未执行，注册中心为空、智能体拿不到任何项目工具。
 *
 * @author zhush
 */
class AgentToolAutoRegistrationTest extends StarterTestSupport {

    @Test
    void agentToolMethodIsRegisteredAfterContextStartup() {
        runner(AgentScopeAutoConfiguration.class)
                .withPropertyValues("zhush.agentscope.enabled=true")
                .withUserConfiguration(TestAgentBeans.class)
                .run(context -> assertThat(context.getBean(ToolkitRegistry.class).all())
                        .extracting(RegisteredTool::getName)
                        .contains("demoEcho"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestAgentBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AgentDefinitionProvider agentDefinitionProvider() {
            return List::of;
        }

        @Bean
        AgentEventListener agentEventListener() {
            return new AgentEventListener() {
                @Override
                public void onEvent(AgentEvent event) {
                    // 测试上下文不消费事件
                }
            };
        }

        @Bean
        DemoTools demoTools() {
            return new DemoTools();
        }
    }

    public static class DemoTools {

        @AgentTool(name = "demoEcho", description = "测试工具：原样返回入参")
        public String echo(String text) {
            return text;
        }
    }
}
