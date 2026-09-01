package com.zhush.agentscope.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhush.agentscope.agent.AgentDefinitionProvider;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.agent.DefaultAgentRegistry;
import com.zhush.agentscope.context.DefaultSessionContextResolver;
import com.zhush.agentscope.context.SessionContextResolver;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.event.AgentEventPublisher;
import com.zhush.agentscope.model.DefaultModelFactory;
import com.zhush.agentscope.safety.DefaultToolApprovalHandler;
import com.zhush.agentscope.safety.NullToolApprovalHandler;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import com.zhush.agentscope.session.AgentSessionManager;
import com.zhush.agentscope.session.AgentSessionStore;
import com.zhush.agentscope.session.MemoryAgentSessionStore;
import com.zhush.agentscope.session.RedisAgentSessionStore;
import com.zhush.agentscope.template.AgentTemplate;
import com.zhush.agentscope.template.DefaultAgentTemplate;
import com.zhush.agentscope.tool.AgentToolScanner;
import com.zhush.agentscope.tool.DefaultToolkitRegistry;
import com.zhush.agentscope.tool.ToolkitRegistry;
import com.zhush.redis.core.RedisCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * AgentScope 智能体框架自动装配。
 *
 * <p>总开关 {@code zhush.agentscope.enabled=true} 时装配全部智能体 Bean；业务模块通过
 * {@link AgentDefinitionProvider} 声明智能体、通过 {@code @AgentTool} 暴露工具即可接入。
 *
 * @author zhush
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentScopeProperties.class)
@ConditionalOnProperty(prefix = "zhush.agentscope", name = "enabled", havingValue = "true")
public class AgentScopeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentSessionStore agentSessionStore(AgentScopeProperties properties,
                                               ObjectProvider<RedisCache> redisCacheProvider,
                                               ObjectMapper objectMapper) {
        RedisCache redis = redisCacheProvider.getIfAvailable();
        boolean redisEnabled = "redis".equalsIgnoreCase(properties.getSessionStore()) && redis != null;
        if (redisEnabled) {
            return new RedisAgentSessionStore(redis, objectMapper, properties.getSessionTtl());
        }
        return new MemoryAgentSessionStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentSessionManager agentSessionManager(AgentSessionStore agentSessionStore) {
        return new AgentSessionManager(agentSessionStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentRegistry agentRegistry(List<AgentDefinitionProvider> providers) {
        return new DefaultAgentRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolkitRegistry toolkitRegistry() {
        return new DefaultToolkitRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentToolScanner agentToolScanner(ToolkitRegistry toolkitRegistry) {
        return new AgentToolScanner(toolkitRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentEventPublisher agentEventPublisher(List<AgentEventListener> listeners) {
        return new AgentEventPublisher(listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionContextResolver sessionContextResolver() {
        return new DefaultSessionContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolApprovalHandler toolApprovalHandler(AgentScopeProperties properties,
                                                   ObjectProvider<RedisCache> redisCacheProvider,
                                                   ObjectMapper objectMapper) {
        RedisCache redis = redisCacheProvider.getIfAvailable();
        if (redis != null) {
            return new DefaultToolApprovalHandler(redis, objectMapper, properties.getSessionTtl());
        }
        return new NullToolApprovalHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultModelFactory defaultModelFactory(AgentScopeProperties properties) {
        return new DefaultModelFactory(properties.getDefaultModel());
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentTemplate agentTemplate(AgentRegistry agentRegistry,
                                       ToolkitRegistry toolkitRegistry,
                                       DefaultModelFactory defaultModelFactory,
                                       AgentSessionManager agentSessionManager,
                                       AgentEventPublisher agentEventPublisher,
                                       SessionContextResolver sessionContextResolver,
                                       ToolApprovalHandler toolApprovalHandler,
                                       AgentScopeProperties properties,
                                       ObjectMapper objectMapper) {
        return new DefaultAgentTemplate(agentRegistry, toolkitRegistry, defaultModelFactory, agentSessionManager,
                agentEventPublisher, sessionContextResolver, toolApprovalHandler, properties, objectMapper);
    }

    /**
     * 容器单例全部就绪后执行工具扫描（业务 Bean 均已实例化）。
     */
    @Bean
    @ConditionalOnMissingBean
    public SmartInitializingSingleton agentToolScannerInitializer(AgentToolScanner agentToolScanner) {
        return agentToolScanner::scan;
    }
}
