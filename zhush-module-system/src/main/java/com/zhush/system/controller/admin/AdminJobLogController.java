package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.job.JobLogService;
import com.zhush.system.query.AdminJobLogQuery;
import com.zhush.system.vo.AdminJobLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务执行记录接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-定时任务执行记录", description = "执行记录的分页查询、详情与删除")
@RequestMapping("/admin/system/job-log")
public class AdminJobLogController {

    private final JobLogService jobLogService;

    public AdminJobLogController(JobLogService jobLogService) {
        this.jobLogService = jobLogService;
    }

    /**
     * 执行记录分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job-log:query")
    @Operation(summary = "执行记录分页")
    public Result<PageResult<AdminJobLogVO>> page(@Valid AdminJobLogQuery query) {
        return Result.success(jobLogService.getJobLogPage(query));
    }

    /**
     * 执行记录详情（含完整异常堆栈）
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job-log:query")
    @Operation(summary = "执行记录详情")
    public Result<AdminJobLogVO> get(@Parameter(description = "记录ID") @PathVariable Long id) {
        return Result.success(jobLogService.getJobLog(id));
    }

    /**
     * 删除执行记录
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:job-log:delete")
    @Idempotent
    @Operation(summary = "删除执行记录")
    public Result<Void> delete(@Parameter(description = "记录ID") @PathVariable Long id) {
        jobLogService.deleteJobLog(id);
        return Result.success();
    }
}
