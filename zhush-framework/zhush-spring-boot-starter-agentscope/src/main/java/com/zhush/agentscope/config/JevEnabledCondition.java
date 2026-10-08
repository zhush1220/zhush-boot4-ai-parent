package com.zhush.agentscope.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Jev 装配守卫：{@code zhush.agentscope.jev.enabled=true} 且 API Key 可得
 * （配置项或环境变量 TYPESAFE_API_KEY / JEV_API_KEY）时才装配 JevClient。
 *
 * <p>条件不满足时 Bean 缺席（而非 null），依赖方一律经 {@code ObjectProvider} 注入并降级，
 * 主链路行为与未集成时一致——对应方案第七章「降级即回退现状」的设计底线。
 *
 * @author zhush
 */
public class JevEnabledCondition implements Condition {

    static final String PREFIX = "zhush.agentscope.jev";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        if (!env.getProperty(PREFIX + ".enabled", Boolean.class, false)) {
            return false;
        }
        if (StringUtils.hasText(env.getProperty(PREFIX + ".api-key"))) {
            return true;
        }
        return StringUtils.hasText(System.getenv("TYPESAFE_API_KEY"))
                || StringUtils.hasText(System.getenv("JEV_API_KEY"));
    }
}
