package com.zhush.agentscope.tool;

import com.zhush.agentscope.context.AgentContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ReflectiveAgentTool} 的 AgentContext 注入契约测试（底座 B1，方案 §一）：
 * <ul>
 *   <li>schema：AgentContext 型形参不出现在 properties/required（不暴露身份给模型）；</li>
 *   <li>注入：RuntimeContext 属性袋里的 AgentContext 原样进入目标方法形参；</li>
 *   <li>兜底：无属性袋时回落匿名管理端身份（userId=0/admin），不抛 NPE；</li>
 *   <li>gate：审批钩子命中即短路，目标方法不执行。</li>
 * </ul>
 */
class ReflectiveAgentToolContextInjectionTest {

    /** 演示工具 Bean：记录框架注入的上下文，供断言取回 */
    static class DemoTool {

        volatile AgentContext seen;

        @SuppressWarnings("unused")
        public String generate(@ToolParam(name = "prompt", description = "提示词") String prompt,
                               AgentContext context) {
            this.seen = context;
            return prompt + ":" + context.getUserId();
        }
    }

    private static RegisteredTool registered(DemoTool bean) throws Exception {
        Method method = DemoTool.class.getMethod("generate", String.class, AgentContext.class);
        return RegisteredTool.builder()
                .name("generate")
                .description("demo")
                .bean(bean)
                .method(method)
                .build();
    }

    private static ReflectiveAgentTool tool(RegisteredTool registered,
                                            java.util.function.Function<ToolCallParam, Optional<ToolResultBlock>> gate) {
        return new ReflectiveAgentTool(registered, new ObjectMapper(), gate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void parametersShouldSkipAgentContextFormal() throws Exception {
        Map<String, Object> schema = tool(registered(new DemoTool()), null).getParameters();

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsOnlyKeys("prompt");
        assertThat((List<String>) schema.get("required")).containsExactly("prompt");
    }

    @Test
    void callAsyncShouldInjectContextFromRuntimeContext() throws Exception {
        DemoTool bean = new DemoTool();
        AgentContext ctx = AgentContext.builder().userId(42L).userType("member").build();
        RuntimeContext rc = RuntimeContext.builder().sessionId("s-1").userId("42").build();
        rc.put(AgentContext.class, ctx);
        ToolCallParam param = mock(ToolCallParam.class);
        when(param.getInput()).thenReturn(Map.of("prompt", "hi"));
        when(param.getRuntimeContext()).thenReturn(rc);

        tool(registered(bean), null).callAsync(param).block();

        assertThat(bean.seen).isSameAs(ctx);
    }

    @Test
    void callAsyncShouldFallbackToAnonymousAdmin() throws Exception {
        DemoTool bean = new DemoTool();
        ToolCallParam param = mock(ToolCallParam.class);
        when(param.getInput()).thenReturn(Map.of("prompt", "hi"));
        when(param.getRuntimeContext()).thenReturn(null);

        tool(registered(bean), null).callAsync(param).block();

        assertThat(bean.seen).isNotNull();
        assertThat(bean.seen.getUserId()).isEqualTo(0L);
        assertThat(bean.seen.getUserType()).isEqualTo("admin");
    }

    @Test
    void gateHitShouldShortCircuitTargetMethod() throws Exception {
        DemoTool bean = new DemoTool();
        ToolCallParam param = mock(ToolCallParam.class);

        ToolResultBlock blocked = tool(registered(bean), p -> Optional.of(ToolResultBlock.text("pending-approval")))
                .callAsync(param)
                .block();

        assertThat(blocked).isNotNull();
        assertThat(bean.seen).isNull();
    }
}
