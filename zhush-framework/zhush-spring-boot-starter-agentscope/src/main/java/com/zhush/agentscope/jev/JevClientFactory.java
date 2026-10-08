package com.zhush.agentscope.jev;

import com.zhush.agentscope.config.AgentScopeProperties;
import io.agentscope.extensions.judge.jev.JevClient;
import io.agentscope.extensions.judge.jev.JevRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * {@link JevClient} 构建工厂：按 {@code zhush.agentscope.jev} 配置组装连接参数。
 *
 * <p>Key 解析口径（方案 17.3）：优先配置项，置空时交由 client 内置环境变量读取序
 * （{@code TYPESAFE_API_KEY} → {@code JEV_API_KEY}）；可用性守卫见 {@code JevEnabledCondition}。
 *
 * @author zhush
 */
public final class JevClientFactory {

    private static final Logger log = LoggerFactory.getLogger(JevClientFactory.class);

    /** 未显式配置重试次数时的缺省退避（与官方 defaults 对齐） */
    private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(500);

    private JevClientFactory() {
    }

    /**
     * 按配置构建 JevClient（调用前需保证开关与 Key 可得性已由条件守卫确认）。
     */
    public static JevClient create(AgentScopeProperties.Jev jev) {
        JevClient.Builder builder = JevClient.builder();
        if (StringUtils.hasText(jev.getApiKey())) {
            builder.apiKey(jev.getApiKey());
        }
        if (StringUtils.hasText(jev.getBaseUrl())) {
            builder.baseUrl(jev.getBaseUrl());
        }
        if (StringUtils.hasText(jev.getModel())) {
            builder.model(jev.getModel());
        }
        if (jev.getTimeout() != null) {
            builder.timeout(jev.getTimeout());
        }
        if (jev.getMaxRetries() >= 0) {
            builder.retryPolicy(new JevRetryPolicy(jev.getMaxRetries(), DEFAULT_INITIAL_BACKOFF));
        }
        log.info("[AgentScope] JevClient 已装配：baseUrl={} model={} timeout={} maxRetries={} failOpen={}",
                jev.getBaseUrl(), jev.getModel(), jev.getTimeout(), jev.getMaxRetries(), jev.isFailOpen());
        return builder.build();
    }
}
