package com.zhush.ai.service;

import com.zhush.agentscope.safety.SensitiveWordMatch;
import com.zhush.agentscope.safety.SensitiveWordResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.SENSITIVE_CONTENT_BLOCKED;
import com.zhush.common.exception.ServiceExceptionUtil;

/**
 * 内容合规闸门：对「自由文本」（对话消息、导出正文、生成 prompt）做敏感词分级检测。
 *
 * <p>与工具调用侧的审批闸门（{@code DefaultAgentTemplate} 命中 {@code @AgentTool(sensitive)} 挂起）互补：
 * 后者作用于「哪个工具被调用」，本闸门作用于「提交的内容本身」。复用 {@link SensitiveWordResolver}
 * 的分级语义：
 * <ul>
 *     <li>{@code LEVEL_NONE / LEVEL_NOTICE}（0/1）—— 放行，命中提示级仅记日志留痕；</li>
 *     <li>{@code LEVEL_APPROVAL / LEVEL_BLOCK}（2/3）—— 内容层无人工续跑通道，统一拒绝并提示修改
 *     （生成/导出是一次同步请求，不能像对话工具那样挂起等待审批，故 2/3 均拦截，避免高成本产物绕过合规）。</li>
 * </ul>
 *
 * @author zhush
 */
@Component
public class ComplianceGuard {

    private static final Logger log = LoggerFactory.getLogger(ComplianceGuard.class);

    private final SensitiveWordResolver sensitiveWordResolver;

    public ComplianceGuard(SensitiveWordResolver sensitiveWordResolver) {
        this.sensitiveWordResolver = sensitiveWordResolver;
    }

    /**
     * 检测文本，命中审批/拦截级即抛业务异常。
     *
     * @param text 待检测文本（null/空放行）
     */
    public void check(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        // toolName 传 null：仅让 matchType=content 的动态词参与内容匹配（yaml 回退作用于工具名，此处不适用）
        SensitiveWordMatch match = sensitiveWordResolver.match(null, text);
        if (!match.isHit()) {
            return;
        }
        if (match.getLevel() >= SensitiveWordMatch.LEVEL_APPROVAL) {
            throw ServiceExceptionUtil.exception(SENSITIVE_CONTENT_BLOCKED, joinWords(match.getWords()));
        }
        // 提示级：放行并留痕
        log.info("[ComplianceGuard] 命中提示级敏感词，放行留痕: {}", joinWords(match.getWords()));
    }

    private String joinWords(List<String> words) {
        return words == null || words.isEmpty() ? "涉敏内容" : String.join("、", words);
    }
}
