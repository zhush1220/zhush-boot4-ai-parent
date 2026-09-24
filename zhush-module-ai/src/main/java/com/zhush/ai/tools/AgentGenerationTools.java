package com.zhush.ai.tools;

import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.service.AiGenerationTaskService;
import com.zhush.ai.service.AiQuotaService;
import com.zhush.ai.service.ComplianceGuard;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.tool.ToolParam;
import com.zhush.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话内图像 / 视频生成工具：把「文生图 / 文生视频」接入 ai-chat 智能体的 ReAct 循环。
 *
 * <p>与文档生成（{@code generateDocument} 同步返回产物）不同，图像/视频是<b>异步</b>能力：
 * 本工具只做「闸门 + 提交」，调用 {@link AiGenerationTaskService#submit} 落任务后立即返回
 * {@code {ok:true, taskId, status:submitted}}；产物由后台线程轮询转存，前端凭 {@code taskId}
 * 轮询 {@code GET /admin/ai/agent/task/{taskId}} 呈现，成功后尽力回填会话消息。
 *
 * <p>三重闸门与对话/导出同口径：能力开关（关则直接拒接，不产生脏任务）→ 内容合规（prompt 敏感词）
 * → 次数配额（按人按任务类型）。视频标记为 {@code sensitive}，命中人工审批策略时由框架挂起确认。
 *
 * <p>末位 {@link AgentContext} 由框架从 {@code RuntimeContext} 注入（含 sessionId/userId/userType），
 * 用于任务归属与产物回填，LLM 不可见、不能自行填写。
 *
 * @author zhush
 */
@Component
public class AgentGenerationTools {

    private static final Logger log = LoggerFactory.getLogger(AgentGenerationTools.class);

    private final AiGenerationTaskService generationTaskService;
    private final ComplianceGuard complianceGuard;
    private final AiQuotaService quotaService;
    private final AgentScopeProperties properties;
    private final ObjectMapper objectMapper;

    public AgentGenerationTools(AiGenerationTaskService generationTaskService,
                                ComplianceGuard complianceGuard,
                                AiQuotaService quotaService,
                                AgentScopeProperties properties,
                                ObjectMapper objectMapper) {
        this.generationTaskService = generationTaskService;
        this.complianceGuard = complianceGuard;
        this.quotaService = quotaService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @AgentTool(name = "generateImage",
            description = "根据文字描述生成图片（异步）：立即返回任务号 taskId，图片稍后生成完成后可在对话中查看。"
                    + "用户要求画图/生成图片/文生图时调用。请回复用户「图片正在生成，请稍候」，不要编造图片链接")
    public String generateImage(
            @ToolParam(name = "prompt", description = "图片内容描述（越具体越好）") String prompt,
            @ToolParam(name = "size", description = "尺寸，形如 1024*1024，可留空用默认") String size,
            @ToolParam(name = "n", description = "生成张数 1-4，可留空默认 1") Integer n,
            AgentContext ctx) {
        return submit(AiTaskType.IMAGE, prompt, size, n, null, ctx);
    }

    @AgentTool(name = "generateVideo", sensitive = true,
            description = "根据文字描述生成短视频（异步、成本较高）：立即返回任务号 taskId，视频稍后生成完成后可在对话中查看。"
                    + "用户要求生成视频/文生视频时调用。请回复用户「视频正在生成，可能需要数分钟」，不要编造视频链接")
    public String generateVideo(
            @ToolParam(name = "prompt", description = "视频内容描述（越具体越好）") String prompt,
            @ToolParam(name = "size", description = "分辨率，形如 1280*720，可留空用默认") String size,
            @ToolParam(name = "duration", description = "时长秒（范围内自动夹紧），可留空用默认") Integer duration,
            AgentContext ctx) {
        return submit(AiTaskType.VIDEO, prompt, size, null, duration, ctx);
    }

    // ================ 内部 ================

    private String submit(AiTaskType type, String prompt, String size, Integer n, Integer duration, AgentContext ctx) {
        if (prompt == null || prompt.isBlank()) {
            return failJson("请提供生成内容描述");
        }
        if (!properties.getGeneration().isEnabled()) {
            return failJson("生成能力未开启，请联系管理员配置生成模型后重试");
        }
        try {
            // 与对话/导出同口径闸门：合规 → 配额，均通过后再提交异步任务
            complianceGuard.check(prompt);
            quotaService.assertAllowed(ctx == null ? null : ctx.getUserId(),
                    ctx == null ? null : ctx.getUserType(), type);
            AiGenerationTaskEntity task = generationTaskService.submit(type, prompt, size, n, duration, ctx);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("taskId", task.getTaskId());
            result.put("type", type.getCode());
            result.put("status", "submitted");
            result.put("hint", type == AiTaskType.VIDEO
                    ? "视频任务已提交，通常需要数分钟生成，请告知用户稍后查看，不要编造链接"
                    : "图片任务已提交，通常需要数十秒生成，请告知用户稍后查看，不要编造链接");
            return objectMapper.writeValueAsString(result);
        } catch (ServiceException e) {
            // 合规/配额/无模型等业务异常：转成结构化失败回给 LLM，由其向用户解释
            return failJson(e.getMessage());
        } catch (Exception e) {
            log.error("[AgentGenerationTools] 生成任务提交失败, type={}", type, e);
            return failJson(e.getMessage());
        }
    }

    private String failJson(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", message == null ? "未知错误" : message);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"生成任务提交失败\"}";
        }
    }
}
