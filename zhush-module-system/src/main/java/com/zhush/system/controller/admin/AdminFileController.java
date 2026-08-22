package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.file.core.DocParseResult;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.system.query.InfraFileQuery;
import com.zhush.system.service.FileService;
import com.zhush.system.vo.InfraFileVO;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端-文件管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-文件管理", description = "文件的上传、解析、删除、详情与分页查询")
@RequestMapping("/admin/system/file")
public class AdminFileController {

    private final FileService fileService;

    public AdminFileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件（上传成功后落库）
     */
    @PostMapping("/upload")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:file:create")
    @Idempotent(timeout = 3, message = "文件正在上传，请勿重复提交")
    @OperateLog(module = "文件管理", type = OperateTypeEnum.CREATE, recordParams = false)
    @Operation(summary = "上传文件")
    public Result<InfraFileVO> upload(@Parameter(description = "文件") @RequestParam("file") MultipartFile file) {
        return Result.success(fileService.uploadFile(file, LoginTypeConstants.ADMIN));
    }

    /**
     * 解析文档：提取文本并自动判断是否存在内嵌图片，存在则上传文件服务，
     * 返回可直接回显富文本编辑器的 HTML 内容与图片访问路径（适用于 Word / PDF 转富文本场景）
     */
    @PostMapping("/parse-doc")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:file:create")
    @Idempotent(timeout = 3, message = "文档正在解析，请勿重复提交")
    @OperateLog(module = "文件管理", type = OperateTypeEnum.CREATE, recordParams = false)
    @Operation(summary = "解析文档（提取文本与图片，输出富文本 HTML）")
    public Result<DocParseResult> parseDoc(@Parameter(description = "文档") @RequestParam("file") MultipartFile file) {
        return Result.success(fileService.parseDoc(file, LoginTypeConstants.ADMIN));
    }

    /**
     * 删除文件（物理删除 + 逻辑删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:file:delete")
    @Idempotent(timeout = 3, message = "文件正在删除，请勿重复提交")
    @OperateLog(module = "文件管理", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除文件")
    public Result<Void> delete(@Parameter(description = "文件ID") @PathVariable Long id) {
        fileService.deleteFile(id, LoginTypeConstants.ADMIN);
        return Result.success();
    }

    /**
     * 文件详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:file:query")
    @Operation(summary = "文件详情")
    public Result<InfraFileVO> get(@Parameter(description = "文件ID") @PathVariable Long id) {
        return Result.success(fileService.getFile(id, LoginTypeConstants.ADMIN));
    }

    /**
     * 文件分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:file:query")
    @Operation(summary = "文件分页")
    public Result<PageResult<InfraFileVO>> page(@Valid InfraFileQuery query) {
        return Result.success(fileService.getFilePage(query, LoginTypeConstants.ADMIN));
    }

}
