package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.generation.AgentDocumentService;
import com.zhush.ai.generation.FileArtifact;
import com.zhush.ai.param.AgentExportParam;
import com.zhush.ai.service.AiQuotaService;
import com.zhush.ai.service.ComplianceGuard;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-智能体文档导出接口：Markdown → Word/PDF，供前端「导出文档」按钮直调
 * （不经过大模型，与对话内 generateDocument 工具共用 {@link AgentDocumentService}）。
 *
 * <p>产物文件按当前登录人归属落 {@code infra_file}；导出结果<b>不写回 {@code ai_chat_message.attachments}</b>——
 * 本接口是对话之外的旁路下载，没有对应的消息轮次，写入历史消息反而会造成「凭空多出一个附件」的误解。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-文档导出", description = "Markdown 渲染 Word/PDF 并返回文件服务下载链接")
@RequestMapping("/admin/ai/agent/export")
public class AgentExportController {

    private final AgentDocumentService documentService;
    private final AiSessionContextResolver contextResolver;
    private final ComplianceGuard complianceGuard;
    private final AiQuotaService quotaService;

    public AgentExportController(AgentDocumentService documentService,
                                AiSessionContextResolver contextResolver,
                                ComplianceGuard complianceGuard,
                                AiQuotaService quotaService) {
        this.documentService = documentService;
        this.contextResolver = contextResolver;
        this.complianceGuard = complianceGuard;
        this.quotaService = quotaService;
    }

    @PostMapping
    @Operation(summary = "导出文档（docx/pdf）")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:assistant")
    @Idempotent(timeout = 10, message = "导出处理中，请勿重复点击")
    public Result<FileArtifact> export(@RequestBody @Valid AgentExportParam param) {
        AgentContext ctx = contextResolver.resolve();
        AiTaskType taskType = "pdf".equalsIgnoreCase(param.getFormat()) ? AiTaskType.PDF : AiTaskType.DOCX;
        // 旁路导出同样过合规与配额（与对话生成同口径），避免绕开闸门
        complianceGuard.check(param.getContent());
        quotaService.assertAllowed(ctx.getUserId(), ctx.getUserType(), taskType);
        FileArtifact artifact = documentService.generate(
                param.getContent(), param.getFormat(), param.getFileName(),
                ctx.getUserType(), ctx.getUserId());
        quotaService.record(ctx.getUserId(), ctx.getUserType(), taskType, 0, 0);
        return Result.success(artifact);
    }
}
