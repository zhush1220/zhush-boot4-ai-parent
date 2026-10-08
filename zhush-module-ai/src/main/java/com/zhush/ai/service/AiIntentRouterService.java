package com.zhush.ai.service;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.param.AgentIntentParam;
import com.zhush.ai.vo.AgentIntentVO;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.session.AgentSessionManager;
import com.zhush.agentscope.session.SessionMessage;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.redis.core.RedisCache;
import io.agentscope.extensions.judge.jev.ChoiceAnswer;
import io.agentscope.extensions.judge.jev.ChoiceQuestion;
import io.agentscope.extensions.judge.jev.JevClient;
import io.agentscope.extensions.judge.jev.SystemOneRequest;
import io.agentscope.extensions.judge.jev.SystemOneResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zhush.ai.enums.AiErrorCode.MESSAGE_TOO_LONG;

/**
 * B1 意图路由（方案第十四章，P0 首发）：taskType 缺省（auto）时用 Jev Choice 结构化决策
 * 做一次七类任务分类，前端「先问再发」，命中后复用既有能力链路（指令注入 → taskType 透传）。
 *
 * <p>降级铁律（方案第七章）：Jev 不可用/异常/超时/低置信一律返回 {@code degraded=true} +
 * chat，HTTP 永远 200——分类失败绝不阻断对话主链路，行为等价于「auto=不注入」的现状。
 *
 * <p>数据出域最小化（方案 17.2 应用层可控面）：state 仅含当前消息 + 会话最近 ≤2 条摘要
 * （各截断 200 字符），不上送完整历史。
 *
 * @author zhush
 */
@Service
public class AiIntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(AiIntentRouterService.class);

    /** 问题标识（Jev questions map 的 key，answers 按此取回） */
    private static final String QUESTION_KEY = "intent";

    /** 会话历史摘要条数与单条截断长度（出域最小化） */
    private static final int HISTORY_TAIL = 2;
    private static final int HISTORY_SNIPPET_LEN = 200;

    /** 粘滞缓存 key 前缀（会话级上轮分类结果） */
    private static final String STICKY_KEY_PREFIX = "ai:jev:sticky:";

    /**
     * 指代/省略跟进特征词：短消息命中这些词时上轮分类大概率仍是其真实意图（如生图后的「再来一张」）。
     */
    private static final Set<String> FOLLOWUP_HINTS = Set.of(
            "再", "换", "也", "就", "你", "他", "她", "它", "这", "那", "上面", "刚才",
            "继续", "还有", "同样", "改成", "调成", "调大", "调小", "一点", "些");

    /**
     * 分类器 criteria：选项 code → 判定描述（枚举级单一口径，方案 14.3「后端落一份 criteria 文本」）。
     * 泛型按 client 签名定为 Map<String,Object>，值统一为描述文案。
     */
    private static final Map<String, Object> INTENT_CRITERIA = new LinkedHashMap<>();

    static {
        INTENT_CRITERIA.put(AiTaskType.CHAT.getCode(),
                "普通提问、闲聊、查询或分析，或意图不明确；拿不准时就选这项（兜底）");
        INTENT_CRITERIA.put(AiTaskType.COPYWRITING.getCode(),
                "要求撰写营销种草文案、推广话术、广告语等可直接投放的推广文本");
        INTENT_CRITERIA.put(AiTaskType.DOCX.getCode(),
                "要求把内容创作并导出为 Word、docx 文档下载");
        INTENT_CRITERIA.put(AiTaskType.PDF.getCode(),
                "要求把内容创作并导出为 PDF 文档下载");
        INTENT_CRITERIA.put(AiTaskType.PARSE.getCode(),
                "要求读取、解析或总结已上传的附件文件内容");
        INTENT_CRITERIA.put(AiTaskType.IMAGE.getCode(),
                "要求画图、生成或创作图片、海报、插画");
        INTENT_CRITERIA.put(AiTaskType.VIDEO.getCode(),
                "要求生成或创作视频、短片");
    }

    private final ObjectProvider<JevClient> jevClientProvider;
    private final ObjectProvider<RedisCache> redisCacheProvider;
    private final AgentSessionManager sessionManager;
    private final AiSessionContextResolver contextResolver;
    private final AgentScopeProperties properties;
    private final JevDecisionRecorder recorder;
    private final AiRateLimiter rateLimiter;

    public AiIntentRouterService(ObjectProvider<JevClient> jevClientProvider,
                                 ObjectProvider<RedisCache> redisCacheProvider,
                                 AgentSessionManager sessionManager,
                                 AiSessionContextResolver contextResolver,
                                 AgentScopeProperties properties,
                                 JevDecisionRecorder recorder,
                                 AiRateLimiter rateLimiter) {
        this.jevClientProvider = jevClientProvider;
        this.redisCacheProvider = redisCacheProvider;
        this.sessionManager = sessionManager;
        this.contextResolver = contextResolver;
        this.properties = properties;
        this.recorder = recorder;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 意图分类入口：限流防刷（Jev 按决策计费，17.3）→ 粘滞复用 → Jev Choice 判定 → 阈值裁决。
     */
    public AgentIntentVO classify(AgentIntentParam param) {
        AgentScopeProperties.Jev jev = properties.getJev();
        validateMessageLength(param.getMessage());
        AgentContext ctx = contextResolver.resolve();
        // 计费面防护：复用对话同款单用户限流（rate-per-minute 缺省即 10 次/分钟）
        rateLimiter.acquire(ctx.getUserId(), ctx.getUserType());

        JevClient client = jevClientProvider.getIfAvailable();
        if (client == null) {
            // Jev 未装配（开关关/无 Key）：静默回落现状行为（auto=chat 不注入）
            return chatDegraded("jev_unavailable", null, false);
        }

        // 粘滞：跟进式短消息复用上轮分类，不请求 Jev（降决策量与计费，方案 14.3）
        String stickyKey = param.getSessionId() == null || param.getSessionId().isBlank()
                ? null : STICKY_KEY_PREFIX + param.getSessionId();
        String stickyIntent = stickyReadable(stickyKey, param.getMessage(), jev.getSticky());
        if (stickyIntent != null) {
            AiTaskType type = AiTaskType.of(stickyIntent);
            record(ctx, param, stickyIntent, null, false, true, 0, null);
            return vo(type, null, false, true);
        }

        long start = System.currentTimeMillis();
        try {
            SystemOneResult result = client.systemOneBlocking(buildRequest(param, jev));
            long cost = System.currentTimeMillis() - start;
            ChoiceAnswer answer = extractChoice(result);
            if (answer == null || answer.choice() == null || answer.choice().isBlank()) {
                record(ctx, param, AiTaskType.CHAT.getCode(), null, true, false, cost, "empty_answer");
                return chatDegraded("empty_answer", answer == null ? null : answer.confidence(), false);
            }
            double confidence = answer.confidence() == null ? 0d : answer.confidence();
            if (confidence < jev.getIntentConfidenceThreshold()) {
                // 低置信：宁不分类不乱分类，回落 chat（分类本身不记降级——Jev 正常应答）
                record(ctx, param, answer.choice(), confidence, false, false, cost, null);
                return vo(AiTaskType.CHAT, confidence, true, false);
            }
            AiTaskType type = AiTaskType.of(answer.choice());
            stickyWrite(stickyKey, type);
            record(ctx, param, type.getCode(), confidence, false, false, cost, null);
            return vo(type, confidence, false, false);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[AiIntentRouter] Jev 决策失败已降级 chat（不阻断主链路）：{}", e.toString());
            record(ctx, param, AiTaskType.CHAT.getCode(), null, true, false, cost, e.toString());
            return chatDegraded("jev_error", null, false);
        }
    }

    /**
     * 组装 Jev 请求：ChoiceQuestion（七类 criteria）+ 最小化 state（当前消息 + ≤2 条历史摘要）。
     */
    private SystemOneRequest buildRequest(AgentIntentParam param, AgentScopeProperties.Jev jev) {
        String instructions = "判断用户下一条消息想要 AI 执行的任务类型，只能从给定选项中选择；"
                + "表达创作并导出文档的诉求时按目标格式选 docx 或 pdf；"
                + "普通提问、意图不明确或仅依赖上下文的简短跟进一律选 chat。";
        ChoiceQuestion question = new ChoiceQuestion(instructions, INTENT_CRITERIA);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("currentMessage", param.getMessage());
        List<Map<String, Object>> history = recentHistory(param.getSessionId());
        if (!history.isEmpty()) {
            state.put("recentHistory", history);
        }
        return SystemOneRequest.builder()
                .state(state)
                .question(QUESTION_KEY, question)
                .model(jev.getModel())
                .build();
    }

    /**
     * 会话最近 {@value HISTORY_TAIL} 条消息摘要（出域最小化：角色 + 截断内容）。
     */
    private List<Map<String, Object>> recentHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        List<SessionMessage> messages = sessionManager.loadHistory(sessionId);
        if (messages.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> tail = new ArrayList<>();
        for (SessionMessage m : messages.subList(Math.max(0, messages.size() - HISTORY_TAIL), messages.size())) {
            if (m.getContent() == null || m.getContent().isBlank()
                    || (!"user".equals(m.getRole()) && !"assistant".equals(m.getRole()))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", m.getRole());
            item.put("content", snippet(m.getContent()));
            tail.add(item);
        }
        return tail;
    }

    private ChoiceAnswer extractChoice(SystemOneResult result) {
        if (result == null || result.answers() == null) {
            return null;
        }
        return result.answers().get(QUESTION_KEY) instanceof ChoiceAnswer choice ? choice : null;
    }

    /**
     * 粘滞命中判定：窗口内（TTL 由缓存保证）+ 短消息 + 含指代/省略特征词 → 复用上轮非 chat 分类。
     */
    private String stickyReadable(String key, String message, AgentScopeProperties.Sticky sticky) {
        if (key == null || message.length() > sticky.getMaxMessageLength()) {
            return null;
        }
        boolean followup = false;
        for (String hint : FOLLOWUP_HINTS) {
            if (message.contains(hint)) {
                followup = true;
                break;
            }
        }
        if (!followup) {
            return null;
        }
        RedisCache redis = redisCacheProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            String last = redis.get(key);
            return last == null || AiTaskType.CHAT.getCode().equals(last) ? null : last;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 仅非 chat 结果入粘滞缓存（纯对话无需复用）。
     */
    private void stickyWrite(String key, AiTaskType type) {
        if (key == null || type == AiTaskType.CHAT) {
            return;
        }
        RedisCache redis = redisCacheProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            Duration ttl = properties.getJev().getSticky().getTtl();
            redis.set(key, type.getCode(), ttl == null || ttl.isZero() || ttl.isNegative()
                    ? Duration.ofMinutes(5) : ttl);
        } catch (Exception e) {
            log.debug("[AiIntentRouter] 粘滞缓存写入失败（忽略）：{}", e.getMessage());
        }
    }

    private void record(AgentContext ctx, AgentIntentParam param, String answer, Double confidence,
                        boolean degraded, boolean sticky, long cost, String errorMsg) {
        recorder.recordIntent(param.getSessionId(), param.getAgentName(), ctx.getUserId(), ctx.getUserType(),
                answer, confidence, degraded, sticky, cost, errorMsg);
    }

    private AgentIntentVO vo(AiTaskType type, Double confidence, boolean degraded, boolean sticky) {
        AgentIntentVO vo = new AgentIntentVO();
        vo.setTaskType(type.getCode());
        vo.setTaskTypeName(type.getDesc());
        vo.setConfidence(confidence == null ? null : BigDecimal.valueOf(confidence));
        vo.setDegraded(degraded);
        vo.setSticky(sticky);
        return vo;
    }

    private AgentIntentVO chatDegraded(String reason, Double confidence, boolean sticky) {
        return vo(AiTaskType.CHAT, confidence, true, sticky);
    }

    private String snippet(String content) {
        String trimmed = content.trim();
        return trimmed.length() <= HISTORY_SNIPPET_LEN ? trimmed : trimmed.substring(0, HISTORY_SNIPPET_LEN);
    }

    private void validateMessageLength(String message) {
        int max = properties.getChat().getMaxMessageLength();
        if (max > 0 && message != null && message.length() > max) {
            throw ServiceExceptionUtil.exception(MESSAGE_TOO_LONG, String.valueOf(max));
        }
    }
}
