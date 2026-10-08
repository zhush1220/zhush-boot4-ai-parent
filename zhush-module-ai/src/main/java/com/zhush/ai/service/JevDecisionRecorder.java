package com.zhush.ai.service;

import com.zhush.ai.entity.AiJevDecisionLogEntity;
import com.zhush.ai.mapper.AiJevDecisionLogMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Jev 决策记录器：异步采样落库 {@code ai_jev_decision_log}（方案 14.4 / 17.3）。
 *
 * <p>采样口径：degraded/sticky 记录全量保留（服务健康指标不留采样盲区），正常决策按
 * {@code zhush.agentscope.jev.log-sample-rate} 概率保留。落库失败静默丢弃并限速告警——
 * 观测面永远不得影响决策与对话主链路。
 *
 * <p>异步线程无登录上下文，{@code create_by/create_time} 在此显式填充（全局填充监听器
 * 依赖 ThreadLocal，跨线程不生效）。
 *
 * @author zhush
 */
@Service
public class JevDecisionRecorder {

    private static final Logger log = LoggerFactory.getLogger(JevDecisionRecorder.class);

    /** 队列容量：观测数据可丢不可堵（满则丢弃并计数） */
    private static final int QUEUE_CAPACITY = 1000;

    private final AiJevDecisionLogMapper decisionLogMapper;
    private final AgentScopeProperties properties;
    private final ExecutorService executor;

    public JevDecisionRecorder(AiJevDecisionLogMapper decisionLogMapper, AgentScopeProperties properties) {
        this.decisionLogMapper = decisionLogMapper;
        this.properties = properties;
        this.executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "jev-decision-recorder");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardPolicy());
    }

    /**
     * 记录一次决策（scene=1 意图路由）。调用方在主线程传入 userId，异步落库。
     */
    public void recordIntent(String sessionId, String agentName, Long userId, String userType,
                             String answer, Double confidence, boolean degraded, boolean sticky,
                             long costMs, String errorMsg) {
        AgentScopeProperties.Jev jev = properties.getJev();
        if (!sample(jev, degraded, sticky)) {
            return;
        }
        AiJevDecisionLogEntity entity = new AiJevDecisionLogEntity();
        entity.setScene(AiJevDecisionLogEntity.SCENE_INTENT);
        entity.setSessionId(sessionId == null ? "" : sessionId);
        entity.setAgentName(agentName == null ? "" : agentName);
        entity.setUserId(userId == null ? 0L : userId);
        entity.setUserType(userType == null ? "admin" : userType);
        entity.setQuestionType("choice");
        entity.setAnswer(answer == null ? "" : answer);
        entity.setConfidence(confidence == null ? null : BigDecimal.valueOf(confidence));
        entity.setDegraded(degraded ? 1 : 0);
        entity.setSticky(sticky ? 1 : 0);
        entity.setCostMs((int) Math.min(costMs, Integer.MAX_VALUE));
        entity.setErrorMsg(truncate(errorMsg));
        entity.setCreateBy(userId);
        entity.setCreateTime(LocalDateTime.now());
        executor.execute(() -> {
            try {
                decisionLogMapper.insert(entity);
            } catch (Exception e) {
                log.warn("[JevDecisionRecorder] 决策记录落库失败（已忽略）：{}", e.getMessage());
            }
        });
    }

    /**
     * 采样判定：降级/粘滞全量；正常记录按配置概率（<=0 关闭正常采样，>=1 全量）。
     */
    private boolean sample(AgentScopeProperties.Jev jev, boolean degraded, boolean sticky) {
        if (degraded || sticky) {
            return true;
        }
        double rate = jev.getLogSampleRate();
        if (rate <= 0) {
            return false;
        }
        if (rate >= 1) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    private String truncate(String msg) {
        if (msg == null || msg.isBlank()) {
            return "";
        }
        return msg.length() <= 500 ? msg : msg.substring(0, 500);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
