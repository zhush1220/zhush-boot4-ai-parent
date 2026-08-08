package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawAuditLogParam;
import com.zhush.union.query.UnionWithdrawAuditLogQuery;
import com.zhush.union.service.UnionWithdrawAuditLogService;
import com.zhush.union.vo.UnionWithdrawAuditLogVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName UnionWithdrawAuditLogController
 * @Description 提现审核日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-提现审核日志", description = "提现审核日志的增删改查")
@RequestMapping("/admin/union/withdraw-audit-log")
public class UnionWithdrawAuditLogController {

    private final UnionWithdrawAuditLogService withdrawAuditLogService;

    public UnionWithdrawAuditLogController(UnionWithdrawAuditLogService withdrawAuditLogService) {
        this.withdrawAuditLogService = withdrawAuditLogService;
    }

    /**
     * 新增提现审核日志
     */
    @PostMapping
    @OperateLog(module = "提现审核日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增提现审核日志")
    public Result<Long> create(@RequestBody @Valid UnionWithdrawAuditLogParam param) {
        return Result.success(withdrawAuditLogService.createWithdrawAuditLog(param));
    }

    /**
     * 修改提现审核日志
     */
    @PutMapping
    @OperateLog(module = "提现审核日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改提现审核日志")
    public Result<Void> update(@RequestBody @Valid UnionWithdrawAuditLogParam param) {
        withdrawAuditLogService.updateWithdrawAuditLog(param);
        return Result.success();
    }

    /**
     * 删除提现审核日志
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "提现审核日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除提现审核日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        withdrawAuditLogService.deleteWithdrawAuditLog(id);
        return Result.success();
    }

    /**
     * 提现审核日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "提现审核日志详情")
    public Result<UnionWithdrawAuditLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(withdrawAuditLogService.getWithdrawAuditLog(id));
    }

    /**
     * 提现审核日志分页
     */
    @GetMapping("/page")
    @Operation(summary = "提现审核日志分页")
    public Result<PageResult<UnionWithdrawAuditLogVO>> page(@Valid UnionWithdrawAuditLogQuery query) {
        return Result.success(withdrawAuditLogService.getWithdrawAuditLogPage(query));
    }

}
