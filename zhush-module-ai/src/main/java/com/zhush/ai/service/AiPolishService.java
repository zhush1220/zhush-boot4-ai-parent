package com.zhush.ai.service;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.param.AgentPolishParam;
import com.zhush.ai.vo.AgentPolishVO;
import com.zhush.agentscope.agent.AgentReply;
import com.zhush.agentscope.template.AgentTemplate;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.common.exception.ServiceExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.zhush.ai.enums.AiErrorCode.MESSAGE_CONTENT_EMPTY;
import static com.zhush.ai.enums.AiErrorCode.MESSAGE_TOO_LONG;
import static com.zhush.ai.enums.AiErrorCode.UPSTREAM_ERROR;

/**
 * 输入优化（润色）服务：对输入框草稿做一次「一次性」大模型改写，帮用户把话说清楚、纠错、调风格。
 *
 * <p>刻意走 {@link AgentTemplate#callOnce}：不落会话、不写历史、不计对话配额，避免污染用户当前对话，
 * 仅复用与对话同口径的限流与内容合规闸门（防刷 + 敏感内容不进模型）。
 *
 * @author zhush
 */
@Service
public class AiPolishService {

    private static final Logger log = LoggerFactory.getLogger(AiPolishService.class);

    private static final String DEFAULT_AGENT = "ai-chat";

    private final AgentTemplate agentTemplate;
    private final AiSessionContextResolver contextResolver;
    private final AiRateLimiter rateLimiter;
    private final ComplianceGuard complianceGuard;
    private final AgentScopeProperties properties;

    public AiPolishService(AgentTemplate agentTemplate, AiSessionContextResolver contextResolver,
                           AiRateLimiter rateLimiter, ComplianceGuard complianceGuard,
                           AgentScopeProperties properties) {
        this.agentTemplate = agentTemplate;
        this.contextResolver = contextResolver;
        this.rateLimiter = rateLimiter;
        this.complianceGuard = complianceGuard;
        this.properties = properties;
    }

    /**
     * 优化入口：校验 → 限流 → 合规 → 组装润色指令 → 一次性调用 → 返回优化文本。
     */
    public AgentPolishVO polish(AgentPolishParam param) {
        String text = param.getText() == null ? "" : param.getText().trim();
        if (text.isEmpty()) {
            throw ServiceExceptionUtil.exception(MESSAGE_CONTENT_EMPTY);
        }
        validateLength(text);
        AgentContext ctx = contextResolver.resolve();
        // 计费/防刷：复用对话同款单用户限流；内容合规：敏感文本不进模型
        rateLimiter.acquire(ctx.getUserId(), ctx.getUserType());
        complianceGuard.check(text);

        String agentName = (param.getAgentName() == null || param.getAgentName().isBlank())
                ? DEFAULT_AGENT : param.getAgentName();
        AgentReply reply = agentTemplate.callOnce(agentName, buildPrompt(text, param));
        if (!reply.isFinished() || reply.getReply() == null || reply.getReply().isBlank()) {
            log.warn("[AiPolish] 润色调用未完成, agent={}, msg={}", agentName, reply.getReply());
            throw ServiceExceptionUtil.exception(UPSTREAM_ERROR, "输入优化失败，请稍后重试");
        }
        AgentPolishVO vo = new AgentPolishVO();
        vo.setOptimized(stripCodeFence(reply.getReply().trim()));
        return vo;
    }

    /**
     * 组装润色指令：保持原意与关键信息，纠错 + 按风格调整；带能力上下文时约束不得改变任务意图；
     * 要求只输出优化后的文本本身（无解释、无引号/代码块），便于前端直接回填。
     */
    private String buildPrompt(String text, AgentPolishParam param) {
        String style = (param.getStyle() == null || param.getStyle().isBlank())
                ? "更清晰、通顺、专业" : param.getStyle().trim();
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名中文文本润色助手。请在严格保持用户原始意图与全部关键信息不变的前提下，")
                .append("优化下面这段文本：纠正错别字与语法、使表达更清晰流畅，并按「").append(style)
                .append("」的风格调整语气。\n");
        AiTaskType taskType = AiTaskType.of(param.getTaskType());
        if (taskType != null && taskType != AiTaskType.CHAT) {
            sb.append("注意：这段文本将用于「").append(taskType.getDesc())
                    .append("」任务，请保持与之匹配的表达方式，不要改变其任务目的。\n");
        }
        sb.append("只输出优化后的文本本身，不要任何解释、前后缀、引号或代码块。\n\n")
                .append("待优化文本：\n").append(text);
        return sb.toString();
    }

    /** 去掉模型偶发包裹的 Markdown 代码块围栏 */
    private String stripCodeFence(String s) {
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            String body = firstNl >= 0 ? s.substring(firstNl + 1) : s;
            if (body.endsWith("```")) {
                body = body.substring(0, body.length() - 3);
            }
            return body.trim();
        }
        return s;
    }

    private void validateLength(String text) {
        int max = properties.getChat().getMaxMessageLength();
        if (max > 0 && text.length() > max) {
            throw ServiceExceptionUtil.exception(MESSAGE_TOO_LONG, String.valueOf(max));
        }
    }
}
