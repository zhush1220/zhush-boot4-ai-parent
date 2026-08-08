package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalTransactionParam;
import com.zhush.union.query.UnionExternalTransactionQuery;
import com.zhush.union.service.UnionExternalTransactionService;
import com.zhush.union.vo.UnionExternalTransactionVO;
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
 * @ClassName UnionExternalTransactionController
 * @Description 外部推广者资金流水表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者资金流水", description = "外部推广者资金流水的增删改查")
@RequestMapping("/admin/union/external-transaction")
public class UnionExternalTransactionController {

    private final UnionExternalTransactionService externalTransactionService;

    public UnionExternalTransactionController(UnionExternalTransactionService externalTransactionService) {
        this.externalTransactionService = externalTransactionService;
    }

    /**
     * 新增外部推广者资金流水
     */
    @PostMapping
    @OperateLog(module = "外部推广者资金流水", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者资金流水")
    public Result<Long> create(@RequestBody @Valid UnionExternalTransactionParam param) {
        return Result.success(externalTransactionService.createExternalTransaction(param));
    }

    /**
     * 修改外部推广者资金流水
     */
    @PutMapping
    @OperateLog(module = "外部推广者资金流水", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者资金流水")
    public Result<Void> update(@RequestBody @Valid UnionExternalTransactionParam param) {
        externalTransactionService.updateExternalTransaction(param);
        return Result.success();
    }

    /**
     * 删除外部推广者资金流水
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "外部推广者资金流水", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者资金流水")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalTransactionService.deleteExternalTransaction(id);
        return Result.success();
    }

    /**
     * 外部推广者资金流水详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "外部推广者资金流水详情")
    public Result<UnionExternalTransactionVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalTransactionService.getExternalTransaction(id));
    }

    /**
     * 外部推广者资金流水分页
     */
    @GetMapping("/page")
    @Operation(summary = "外部推广者资金流水分页")
    public Result<PageResult<UnionExternalTransactionVO>> page(@Valid UnionExternalTransactionQuery query) {
        return Result.success(externalTransactionService.getExternalTransactionPage(query));
    }

}
