package com.zhush.agentscope.context;

/**
 * 默认上下文解析器：无法从请求上下文解析时返回匿名上下文（租户 id=0）。
 *
 * <p>业务侧（如管理端对接 Sa-Token）可另外注册一个 {@link SessionContextResolver} Bean，
 * 框架通过 {@code @ConditionalOnMissingBean} 优先使用业务实现。
 *
 * @author zhush
 */
public class DefaultSessionContextResolver implements SessionContextResolver {

    @Override
    public AgentContext resolve() {
        return AgentContext.builder()
                .tenantId("0")
                .build();
    }
}
