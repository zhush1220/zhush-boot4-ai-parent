package com.zhush.system.controller.app;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.system.query.InfraFileQuery;
import com.zhush.system.service.FileService;
import com.zhush.system.vo.InfraFileVO;
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
 * 会员端-文件接口
 *
 * <p>会员端仅可上传、查询、删除本人上传的文件（由 FileService 统一校验归属）。
 * 登录态校验由 Sa-Token 拦截器按 {@code /member/**} 前缀统一处理。
 *
 * @author zhush
 */
@RestController
@Tag(name = "会员端-文件", description = "会员文件的上传、删除、详情与分页查询（仅限本人）")
@RequestMapping("/member/file")
public class AppFileController {

    private final FileService fileService;

    public AppFileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件（上传成功后落库）
     */
    @PostMapping("/upload")
    @Idempotent(timeout = 3, message = "文件正在上传，请勿重复提交")
    @Operation(summary = "上传文件")
    public Result<InfraFileVO> upload(@Parameter(description = "文件") @RequestParam("file") MultipartFile file) {
        return Result.success(fileService.uploadFile(file, LoginTypeConstants.MEMBER));
    }

    /**
     * 删除文件（仅限本人上传的文件）
     */
    @DeleteMapping("/{id}")
    @Idempotent(timeout = 3, message = "文件正在删除，请勿重复提交")
    @Operation(summary = "删除文件")
    public Result<Void> delete(@Parameter(description = "文件ID") @PathVariable Long id) {
        fileService.deleteFile(id, LoginTypeConstants.MEMBER);
        return Result.success();
    }

    /**
     * 文件详情（仅限本人上传的文件）
     */
    @GetMapping("/{id}")
    @Operation(summary = "文件详情")
    public Result<InfraFileVO> get(@Parameter(description = "文件ID") @PathVariable Long id) {
        return Result.success(fileService.getFile(id, LoginTypeConstants.MEMBER));
    }

    /**
     * 我的文件分页（强制限定为本人上传的文件）
     */
    @GetMapping("/page")
    @Operation(summary = "我的文件分页")
    public Result<PageResult<InfraFileVO>> page(@Valid InfraFileQuery query) {
        return Result.success(fileService.getFilePage(query, LoginTypeConstants.MEMBER));
    }

}
