package com.zhush.agentscope.config;

import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.safety.SensitiveWordResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 动态配置刷新协调器：统一驱动模型池/敏感词库缓存刷新与 Agent 缓存失效。
 *
 * <p>三级刷新机制：
 * <ul>
 *     <li>L1 本实例：业务变更成功后直接调 {@link #refreshAll()}；</li>
 *     <li>L2 多实例：Redis pub/sub（频道 {@link #REFRESH_CHANNEL}）订阅方调 {@link #refreshAll()}；</li>
 *     <li>L3 定时兜底：按 {@code dynamic-config.refresh-interval} 周期全量重载。</li>
 * </ul>
 *
 * @author zhush
 */
public class DynamicConfigRefreshCoordinator implements DisposableBean {

    /** 配置刷新广播频道 */
    public static final String REFRESH_CHANNEL = "agentscope:config:refresh";

    private static final Logger log = LoggerFactory.getLogger(DynamicConfigRefreshCoordinator.class);

    private final ObjectProvider<AgentModelResolver> modelResolver;
    private final ObjectProvider<SensitiveWordResolver> sensitiveWordResolver;
    private final List<Runnable> evictionListeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;

    /**
     * 协调器通过 {@link ObjectProvider} 惰性获取解析器（避免与 Provider→Service→Coordinator 的 Bean 循环依赖）。
     */
    public DynamicConfigRefreshCoordinator(ObjectProvider<AgentModelResolver> modelResolver,
                                           ObjectProvider<SensitiveWordResolver> sensitiveWordResolver) {
        this.modelResolver = modelResolver;
        this.sensitiveWordResolver = sensitiveWordResolver;
    }

    /**
     * 注册缓存失效监听（如 AgentTemplate 的 HarnessAgent 缓存清除）。
     */
    public void addEvictionListener(Runnable listener) {
        evictionListeners.add(listener);
    }

    /**
     * 全量刷新：模型池 + 敏感词库 + 触发缓存失效。
     */
    public void refreshAll() {
        try {
            AgentModelResolver modelResolver = this.modelResolver.getIfAvailable();
            if (modelResolver != null) {
                modelResolver.refresh();
            }
            SensitiveWordResolver sensitiveWordResolver = this.sensitiveWordResolver.getIfAvailable();
            if (sensitiveWordResolver != null) {
                sensitiveWordResolver.refresh();
            }
            for (Runnable listener : evictionListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    log.warn("Agent 缓存失效执行异常", e);
                }
            }
        } catch (Exception e) {
            log.warn("动态配置刷新异常", e);
        }
    }

    /**
     * 启动定时兜底刷新。
     */
    public void startScheduled(Duration interval) {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "agentscope-config-refresh");
            thread.setDaemon(true);
            return thread;
        });
        long seconds = interval.toSeconds();
        scheduler.scheduleWithFixedDelay(this::refreshAll, seconds, seconds, TimeUnit.SECONDS);
        log.info("动态配置定时兜底刷新已启动, 周期={}s", seconds);
    }

    @Override
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
