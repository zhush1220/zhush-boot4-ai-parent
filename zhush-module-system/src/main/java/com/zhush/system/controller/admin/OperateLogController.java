package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.query.OperateLogQuery;
import com.zhush.system.service.OperateLogService;
import com.zhush.system.vo.OperateLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-操作日志", description = "操作日志的分页查询与详情")
@RequestMapping("/admin/system/operate-log")
public class OperateLogController {

    private final OperateLogService operateLogService;

    public OperateLogController(OperateLogService operateLogService) {
        this.operateLogService = operateLogService;
    }

    /**
     * 操作日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:operate-log:query")
    @Operation(summary = "操作日志分页")
    public Result<PageResult<OperateLogVO>> page(@Valid OperateLogQuery query) {
        return Result.success(operateLogService.getOperateLogPage(query));
    }

    /**
     * 操作日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:operate-log:query")
    @Operation(summary = "操作日志详情")
    public Result<OperateLogVO> get(@Parameter(description = "日志ID") @PathVariable Long id) {
        return Result.success(operateLogService.getOperateLog(id));
    }

}
