package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.query.SqlAuditLogQuery;
import com.zhush.system.service.SqlAuditLogService;
import com.zhush.system.vo.SqlAuditLogVO;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
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
 * SQL 审计日志接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-SQL审计日志", description = "SQL 审计日志的分页查询、详情与删除")
@RequestMapping("/admin/system/sql-audit")
public class AdminSqlAuditLogController {

    private final SqlAuditLogService sqlAuditLogService;

    public AdminSqlAuditLogController(SqlAuditLogService sqlAuditLogService) {
        this.sqlAuditLogService = sqlAuditLogService;
    }

    /**
     * SQL 审计日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:sql-audit:query")
    @Operation(summary = "SQL 审计日志分页")
    public Result<PageResult<SqlAuditLogVO>> page(@Valid SqlAuditLogQuery query) {
        return Result.success(sqlAuditLogService.getSqlAuditPage(query));
    }

    /**
     * SQL 审计日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:sql-audit:query")
    @Operation(summary = "SQL 审计日志详情")
    public Result<SqlAuditLogVO> get(@Parameter(description = "日志ID") @PathVariable Long id) {
        return Result.success(sqlAuditLogService.getSqlAudit(id));
    }

    /**
     * 删除 SQL 审计日志
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:sql-audit:delete")
    @OperateLog(module = "SQL审计日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除 SQL 审计日志")
    public Result<Void> delete(@Parameter(description = "日志ID") @PathVariable Long id) {
        sqlAuditLogService.deleteSqlAudit(id);
        return Result.success();
    }

}
