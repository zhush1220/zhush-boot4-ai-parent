package com.zhush.agentscope.config;

import tools.jackson.databind.ObjectMapper;
import com.zhush.agentscope.agent.AgentDefinitionProvider;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.agent.DefaultAgentRegistry;
import com.zhush.agentscope.context.DefaultSessionContextResolver;
import com.zhush.agentscope.context.SessionContextResolver;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.event.AgentEventPublisher;
import com.zhush.agentscope.event.AgentResumeChannels;
import com.zhush.agentscope.event.AgentTraceIdSupplier;
import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.model.DefaultModelFactory;
import com.zhush.agentscope.model.ModelConfigProvider;
import com.zhush.agentscope.safety.DefaultToolApprovalHandler;
import com.zhush.agentscope.safety.ApprovalPersister;
import com.zhush.agentscope.safety.NullToolApprovalHandler;
import com.zhush.agentscope.safety.SensitiveWordProvider;
import com.zhush.agentscope.safety.SensitiveWordResolver;
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
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.extensions.redis.state.redisson.RedissonAgentStateStore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

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
    public AgentEventPublisher agentEventPublisher(List<AgentEventListener> listeners,
                                                  AgentScopeProperties properties,
                                                  ObjectProvider<AgentTraceIdSupplier> traceIdProvider) {
        // 默认异步分发：审计/用量监听器的落库不得阻塞 SSE 尾包（线程池随 Bean 关闭优雅停止）
        return new AgentEventPublisher(listeners, properties, traceIdProvider.getIfAvailable());
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
    public AgentModelResolver agentModelResolver(ObjectProvider<ModelConfigProvider> providers,
                                                 AgentScopeProperties properties) {
        List<ModelConfigProvider> list = properties.getDynamicConfig().isEnabled()
                ? providers.orderedStream().toList() : List.of();
        return new AgentModelResolver(list, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordResolver sensitiveWordResolver(ObjectProvider<SensitiveWordProvider> providers,
                                                       AgentScopeProperties properties) {
        List<SensitiveWordProvider> list = properties.getDynamicConfig().isEnabled()
                ? providers.orderedStream().toList() : List.of();
        return new SensitiveWordResolver(list, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicConfigRefreshCoordinator dynamicConfigRefreshCoordinator(
            ObjectProvider<AgentModelResolver> modelResolver,
            ObjectProvider<SensitiveWordResolver> sensitiveWordResolver,
            AgentScopeProperties properties) {
        DynamicConfigRefreshCoordinator coordinator =
                new DynamicConfigRefreshCoordinator(modelResolver, sensitiveWordResolver);
        if (properties.getDynamicConfig().isEnabled()) {
            coordinator.startScheduled(properties.getDynamicConfig().getRefreshInterval());
        }
        return coordinator;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentResumeChannels agentResumeChannels() {
        // 路线 A/B 共用的会话续跑 SSE 通道（单实例内存；多实例需替换为 Redis pub/sub）
        return new AgentResumeChannels();
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
                                       ObjectMapper objectMapper,
                                       AgentModelResolver modelResolver,
                                       SensitiveWordResolver sensitiveWordResolver,
                                       ObjectProvider<ApprovalPersister> approvalPersisterProvider,
                                       ObjectProvider<RedissonClient> redissonClientProvider,
                                       AgentResumeChannels resumeChannels,
                                       DynamicConfigRefreshCoordinator refreshCoordinator) {
        // 路线 B 状态存储：仅 nativeState.enabled 且 Redisson 可用时装配，否则为 null
        // （buildHarness 检测到 null 会回退旧版无状态实例，新会话自动降级为 toMsgs 全量重发）
        AgentStateStore stateStore = buildAgentStateStore(properties, redissonClientProvider.getIfAvailable());
        DefaultAgentTemplate template = new DefaultAgentTemplate(agentRegistry, toolkitRegistry,
                defaultModelFactory, agentSessionManager, agentEventPublisher, sessionContextResolver,
                toolApprovalHandler, properties, objectMapper, modelResolver, sensitiveWordResolver,
                approvalPersisterProvider.getIfAvailable(), stateStore, resumeChannels);
        refreshCoordinator.addEvictionListener(template::evictAllAgentCache);
        return template;
    }

    /**
     * 构建 AgentScope 原生状态存储（Redisson）。未开启 {@code nativeState.enabled} 或无 Redisson 时返回 null，
     * 由 {@link DefaultAgentTemplate} 按会话来源位降级，不影响旧会话。
     */
    private AgentStateStore buildAgentStateStore(AgentScopeProperties properties, RedissonClient redissonClient) {
        AgentScopeProperties.NativeState nativeState = properties.getNativeState();
        if (nativeState == null || !nativeState.isEnabled()) {
            return null;
        }
        if (redissonClient == null) {
            if (properties.getSafety().isFailOnApprovalStoreUnavailable()) {
                throw new IllegalStateException("nativeState.enabled=true 但 RedissonClient 未装配，"
                        + "failOnApprovalStoreUnavailable=true 要求 fail-fast，请启用 Redis 或关闭原生状态");
            }
            return null;
        }
        return RedissonAgentStateStore.builder()
                .redissonClient(redissonClient)
                .keyPrefix(nativeState.getKeyPrefix() == null ? "agentscope:state:" : nativeState.getKeyPrefix())
                .build();
    }

    /**
     * 多实例配置刷新广播订阅（Redis pub/sub，频道 {@link DynamicConfigRefreshCoordinator#REFRESH_CHANNEL}）。
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "zhush.agentscope.dynamic-config", name = "broadcast",
            havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer agentConfigRefreshListenerContainer(
            RedisConnectionFactory connectionFactory,
            DynamicConfigRefreshCoordinator refreshCoordinator) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> refreshCoordinator.refreshAll(),
                new ChannelTopic(DynamicConfigRefreshCoordinator.REFRESH_CHANNEL));
        return container;
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
