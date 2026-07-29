package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.query.OpenApiLogQuery;
import com.zhush.system.service.OpenApiLogService;
import com.zhush.system.vo.OpenApiLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放 API 调用日志接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-开放API日志", description = "第三方开放 API 调用日志的分页查询与详情")
@RequestMapping("/admin/system/open-api-log")
public class OpenApiLogController {

    private final OpenApiLogService openApiLogService;

    public OpenApiLogController(OpenApiLogService openApiLogService) {
        this.openApiLogService = openApiLogService;
    }

    /**
     * 调用日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:open-api-log:query")
    @Operation(summary = "调用日志分页")
    public Result<PageResult<OpenApiLogVO>> page(@Valid OpenApiLogQuery query) {
        return Result.success(openApiLogService.getOpenApiLogPage(query));
    }

    /**
     * 调用日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:open-api-log:query")
    @Operation(summary = "调用日志详情")
    public Result<OpenApiLogVO> get(@Parameter(description = "日志ID") @PathVariable Long id) {
        return Result.success(openApiLogService.getOpenApiLog(id));
    }

}
