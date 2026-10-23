package com.zhush.agentscope.config;

import com.zhush.agentscope.websearch.MockWebSearchProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 联网搜索装配守卫：{@code zhush.agentscope.web-search.enabled=true} 且密钥可得
 * （配置项或环境变量 {@code WEB_SEARCH_API_KEY}）时才装配 Provider/Fetcher/Service；
 * {@code provider=mock} 无需密钥（链路验证与 CI 用）。
 *
 * <p>条件不满足时 Bean 缺席，工具经 {@code ObjectProvider} 注入并降级为「能力未开启」，
 * 与 {@link JevEnabledCondition} 同构——对应方案 6.7 降级矩阵第一行。
 *
 * @author zhush
 */
public class WebSearchEnabledCondition implements Condition {

    static final String PREFIX = "zhush.agentscope.web-search";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        if (!env.getProperty(PREFIX + ".enabled", Boolean.class, false)) {
            return false;
        }
        String provider = env.getProperty(PREFIX + ".provider", "iqs");
        if (MockWebSearchProvider.CODE.equalsIgnoreCase(provider)) {
            return true;
        }
        return StringUtils.hasText(env.getProperty(PREFIX + ".api-key"))
                || StringUtils.hasText(System.getenv("WEB_SEARCH_API_KEY"));
    }
}
