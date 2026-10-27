package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.zhush.ai.param.AgentFileResolveParam;
import com.zhush.ai.preview.AgentFilePreviewService;
import com.zhush.ai.preview.AnswerFileArtifactService;
import com.zhush.ai.vo.AnswerFileVO;
import com.zhush.ai.vo.FilePreviewMetaVO;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.zhush.ai.enums.AiErrorCode.FILE_CONVERT_FAILED;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * AI 会话产物预览接口（管理端）。
 *
 * <p>路线乙（{@code office-converter=none}）只吐元信息不代理文件字节：文件仍由静态资源映射直出，
 * 前端凭 {@code previewMode} 选渲染器。路线甲多一条 {@code preview-content}：转换产物落在
 * {@code /api/files/**} 之外，必须由本接口带归属校验下发。
 *
 * <p>{@code preview-meta} 与 {@code preview-content} 都不接受任意 URL，只认 {@code fileId}，
 * 从根上堵住预览侧 SSRF（方案 S6）。{@code answer-file/resolve} 是唯一的例外入口，但它也不收 URL：
 * 只接受工作区相对路径，且强制解析到<b>调用者自己</b>的工作区分区内，见
 * {@link AnswerFileArtifactService} 的三条安全口径。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI产物预览", description = "会话产物在线预览元信息裁决")
@RequestMapping("/admin/ai/agent")
public class AgentFilePreviewController {

    private final AgentFilePreviewService previewService;
    private final AnswerFileArtifactService answerFileService;

    public AgentFilePreviewController(AgentFilePreviewService previewService,
                                      AnswerFileArtifactService answerFileService) {
        this.previewService = previewService;
        this.answerFileService = answerFileService;
    }

    /**
     * 产物预览元信息
     */
    @GetMapping("/file/{fileId}/preview-meta")
    @Operation(summary = "产物预览元信息")
    // 与会话消息接口同一权限口径：助手自用（ai:assistant）或会话记录页（ai:session:list）
    @SaCheckPermission(type = CommonConstant.TYPE, mode = SaMode.OR, value = {"ai:assistant", "ai:session:list"})
    @OperateLog(module = "AI助手", name = "预览会话产物", type = OperateTypeEnum.QUERY, recordResult = false)
    public Result<FilePreviewMetaVO> previewMeta(
            @Parameter(description = "文件记录ID") @PathVariable Long fileId,
            @Parameter(description = "会话ID，传入可跳过反查直接校验归属")
            @RequestParam(required = false) String sessionId) {
        return Result.success(previewService.previewMeta(fileId, sessionId));
    }

    /**
     * 服务端转换产物（PDF）下发：仅在 {@code office-converter} 非 none 时被前端用到。
     *
     * <p>字节来自本模块缓存目录，不走免鉴权的 {@code /api/files/**}，故每次读取都重跑一遍会话归属断言。
     */
    @GetMapping("/file/{fileId}/preview-content")
    @Operation(summary = "服务端转换产物下发（PDF）")
    @SaCheckPermission(type = CommonConstant.TYPE, mode = SaMode.OR, value = {"ai:assistant", "ai:session:list"})
    public ResponseEntity<Resource> previewContent(
            @Parameter(description = "文件记录ID") @PathVariable Long fileId,
            @Parameter(description = "会话ID") @RequestParam(required = false) String sessionId) {
        AgentFilePreviewService.ConvertedPdf converted = previewService.convertedPdf(fileId, sessionId);
        FileSystemResource resource = new FileSystemResource(converted.file());
        long length;
        try {
            length = resource.contentLength();
        } catch (IOException e) {
            throw exception(FILE_CONVERT_FAILED, fileId);
        }
        HttpHeaders headers = new HttpHeaders();
        // 前端用 fetch 取 Blob 再交 pdf.js，不给 Range：整份取回，避免为缓存文件实现分段
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(length);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(converted.name(), StandardCharsets.UTF_8)
                .build());
        headers.add("X-Content-Type-Options", "nosniff");
        // 带归属校验的产物字节不做 HTTP 缓存：URL 只含 fileId+sessionId，命中缓存即跳过本接口的归属断言
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    /**
     * 登记回答正文引用的文件为可预览产物：智能体工作区里的产物（harness 内置文件能力写的）
     * 既无 {@code fileId} 也无 URL，凭正文路径按需读出来入库、补写会话附件，之后走同一套预览裁决。
     *
     * <p>用 POST 而非 GET：它有副作用（写 {@code infra_file} 与 {@code ai_chat_message.attachments}），
     * 且引用串可能较长。
     */
    @PostMapping("/answer-file/resolve")
    @Operation(summary = "登记回答正文引用的文件（工作区产物）")
    @SaCheckPermission(type = CommonConstant.TYPE, mode = SaMode.OR, value = {"ai:assistant", "ai:session:list"})
    @OperateLog(module = "AI助手", name = "登记回答引用文件", type = OperateTypeEnum.CREATE, recordResult = false)
    public Result<AnswerFileVO> resolveAnswerFile(@RequestBody @Valid AgentFileResolveParam param) {
        return Result.success(answerFileService.resolve(param));
    }
}
