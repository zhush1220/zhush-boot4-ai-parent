package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminJobParam;
import com.zhush.system.param.AdminJobStatusParam;
import com.zhush.system.query.AdminJobQuery;
import com.zhush.system.service.AdminJobService;
import com.zhush.system.vo.AdminJobVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定时任务管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-定时任务管理", description = "定时任务的增删改查、启动/停止、执行一次")
@RequestMapping("/admin/system/job")
public class AdminJobController {

    private final AdminJobService adminJobService;

    public AdminJobController(AdminJobService adminJobService) {
        this.adminJobService = adminJobService;
    }

    /**
     * 新增定时任务
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:create")
    @Idempotent
    @Operation(summary = "新增定时任务")
    public Result<Long> create(@RequestBody @Valid AdminJobParam param) {
        return Result.success(adminJobService.createJob(param));
    }

    /**
     * 修改定时任务
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:update")
    @Idempotent
    @Operation(summary = "修改定时任务")
    public Result<Void> update(@RequestBody @Valid AdminJobParam param) {
        adminJobService.updateJob(param);
        return Result.success();
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:delete")
    @Idempotent
    @Operation(summary = "删除定时任务")
    public Result<Void> delete(@Parameter(description = "任务ID") @PathVariable Long id) {
        adminJobService.deleteJob(id);
        return Result.success();
    }

    /**
     * 批量删除定时任务
     */
    @DeleteMapping("/batch")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:delete")
    @Idempotent
    @Operation(summary = "批量删除定时任务")
    public Result<Void> deleteBatch(@Parameter(description = "任务ID列表") @RequestBody List<Long> ids) {
        adminJobService.deleteJobBatch(ids);
        return Result.success();
    }

    /**
     * 任务详情（含下次执行时间）
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:query")
    @Operation(summary = "定时任务详情")
    public Result<AdminJobVO> get(@Parameter(description = "任务ID") @PathVariable Long id) {
        return Result.success(adminJobService.getJob(id));
    }

    /**
     * 任务分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:query")
    @Operation(summary = "定时任务分页")
    public Result<PageResult<AdminJobVO>> page(@Valid AdminJobQuery query) {
        return Result.success(adminJobService.getJobPage(query));
    }

    /**
     * 启动 / 停止任务
     */
    @PutMapping("/status")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:change-status")
    @Idempotent
    @Operation(summary = "启动/停止定时任务")
    public Result<Void> changeStatus(@RequestBody @Valid AdminJobStatusParam param) {
        adminJobService.changeStatus(param.getId(), param.getStatus());
        return Result.success();
    }

    /**
     * 立即执行一次
     */
    @PutMapping("/trigger/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job:trigger")
    @Operation(summary = "立即执行一次定时任务")
    public Result<Void> trigger(@Parameter(description = "任务ID") @PathVariable Long id) {
        adminJobService.triggerJob(id);
        return Result.success();
    }
}
