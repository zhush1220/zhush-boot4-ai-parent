package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskExecutionLogParam;
import com.zhush.union.query.UnionRiskExecutionLogQuery;
import com.zhush.union.service.UnionRiskExecutionLogService;
import com.zhush.union.vo.UnionRiskExecutionLogVO;
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
 * @ClassName UnionRiskExecutionLogController
 * @Description 风控执行记录表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-风控执行记录", description = "风控执行记录的增删改查")
@RequestMapping("/admin/union/risk-execution-log")
public class UnionRiskExecutionLogController {

    private final UnionRiskExecutionLogService riskExecutionLogService;

    public UnionRiskExecutionLogController(UnionRiskExecutionLogService riskExecutionLogService) {
        this.riskExecutionLogService = riskExecutionLogService;
    }

    /**
     * 新增风控执行记录
     */
    @PostMapping
    @OperateLog(module = "风控执行记录", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增风控执行记录")
    public Result<Long> create(@RequestBody @Valid UnionRiskExecutionLogParam param) {
        return Result.success(riskExecutionLogService.createRiskExecutionLog(param));
    }

    /**
     * 修改风控执行记录
     */
    @PutMapping
    @OperateLog(module = "风控执行记录", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改风控执行记录")
    public Result<Void> update(@RequestBody @Valid UnionRiskExecutionLogParam param) {
        riskExecutionLogService.updateRiskExecutionLog(param);
        return Result.success();
    }

    /**
     * 删除风控执行记录
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "风控执行记录", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除风控执行记录")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        riskExecutionLogService.deleteRiskExecutionLog(id);
        return Result.success();
    }

    /**
     * 风控执行记录详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "风控执行记录详情")
    public Result<UnionRiskExecutionLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(riskExecutionLogService.getRiskExecutionLog(id));
    }

    /**
     * 风控执行记录分页
     */
    @GetMapping("/page")
    @Operation(summary = "风控执行记录分页")
    public Result<PageResult<UnionRiskExecutionLogVO>> page(@Valid UnionRiskExecutionLogQuery query) {
        return Result.success(riskExecutionLogService.getRiskExecutionLogPage(query));
    }

}
