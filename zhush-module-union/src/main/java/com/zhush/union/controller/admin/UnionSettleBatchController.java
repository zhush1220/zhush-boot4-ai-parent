package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionSettleBatchParam;
import com.zhush.union.query.UnionSettleBatchQuery;
import com.zhush.union.service.UnionSettleBatchService;
import com.zhush.union.vo.UnionSettleBatchVO;
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
 * @ClassName UnionSettleBatchController
 * @Description 结算批次表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-结算批次", description = "结算批次的增删改查")
@RequestMapping("/admin/union/settle-batch")
public class UnionSettleBatchController {

    private final UnionSettleBatchService settleBatchService;

    public UnionSettleBatchController(UnionSettleBatchService settleBatchService) {
        this.settleBatchService = settleBatchService;
    }

    /**
     * 新增结算批次
     */
    @PostMapping
    @OperateLog(module = "结算批次", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增结算批次")
    public Result<Long> create(@RequestBody @Valid UnionSettleBatchParam param) {
        return Result.success(settleBatchService.createSettleBatch(param));
    }

    /**
     * 修改结算批次
     */
    @PutMapping
    @OperateLog(module = "结算批次", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改结算批次")
    public Result<Void> update(@RequestBody @Valid UnionSettleBatchParam param) {
        settleBatchService.updateSettleBatch(param);
        return Result.success();
    }

    /**
     * 删除结算批次
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "结算批次", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除结算批次")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        settleBatchService.deleteSettleBatch(id);
        return Result.success();
    }

    /**
     * 结算批次详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "结算批次详情")
    public Result<UnionSettleBatchVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(settleBatchService.getSettleBatch(id));
    }

    /**
     * 结算批次分页
     */
    @GetMapping("/page")
    @Operation(summary = "结算批次分页")
    public Result<PageResult<UnionSettleBatchVO>> page(@Valid UnionSettleBatchQuery query) {
        return Result.success(settleBatchService.getSettleBatchPage(query));
    }

}
