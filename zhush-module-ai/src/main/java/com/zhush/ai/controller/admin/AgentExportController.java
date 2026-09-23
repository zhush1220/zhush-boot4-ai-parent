package com.zhush.ai.controller.admin;

import com.zhush.ai.generation.AgentDocumentService;
import com.zhush.ai.generation.FileArtifact;
import com.zhush.ai.param.AgentExportParam;
import com.zhush.common.response.Result;
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
 * @author zhush
 */
@RestController
@Tag(name = "管理端-文档导出", description = "Markdown 渲染 Word/PDF 并返回文件服务下载链接")
@RequestMapping("/admin/ai/agent/export")
public class AgentExportController {

    private final AgentDocumentService documentService;

    public AgentExportController(AgentDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(summary = "导出文档（docx/pdf）")
    public Result<FileArtifact> export(@RequestBody @Valid AgentExportParam param) {
        return Result.success(documentService.generate(
                param.getContent(), param.getFormat(), param.getFileName()));
    }
}
