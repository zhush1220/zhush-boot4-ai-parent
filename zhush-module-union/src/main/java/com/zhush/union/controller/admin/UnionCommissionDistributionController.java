package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionDistributionParam;
import com.zhush.union.query.UnionCommissionDistributionQuery;
import com.zhush.union.service.UnionCommissionDistributionService;
import com.zhush.union.vo.UnionCommissionDistributionVO;
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
 * @ClassName UnionCommissionDistributionController
 * @Description 佣金分配明细表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-佣金分配明细", description = "佣金分配明细的增删改查")
@RequestMapping("/admin/union/commission-distribution")
public class UnionCommissionDistributionController {

    private final UnionCommissionDistributionService commissionDistributionService;

    public UnionCommissionDistributionController(UnionCommissionDistributionService commissionDistributionService) {
        this.commissionDistributionService = commissionDistributionService;
    }

    /**
     * 新增佣金分配明细
     */
    @PostMapping
    @OperateLog(module = "佣金分配明细", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增佣金分配明细")
    public Result<Long> create(@RequestBody @Valid UnionCommissionDistributionParam param) {
        return Result.success(commissionDistributionService.createCommissionDistribution(param));
    }

    /**
     * 修改佣金分配明细
     */
    @PutMapping
    @OperateLog(module = "佣金分配明细", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改佣金分配明细")
    public Result<Void> update(@RequestBody @Valid UnionCommissionDistributionParam param) {
        commissionDistributionService.updateCommissionDistribution(param);
        return Result.success();
    }

    /**
     * 删除佣金分配明细
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "佣金分配明细", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除佣金分配明细")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        commissionDistributionService.deleteCommissionDistribution(id);
        return Result.success();
    }

    /**
     * 佣金分配明细详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "佣金分配明细详情")
    public Result<UnionCommissionDistributionVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(commissionDistributionService.getCommissionDistribution(id));
    }

    /**
     * 佣金分配明细分页
     */
    @GetMapping("/page")
    @Operation(summary = "佣金分配明细分页")
    public Result<PageResult<UnionCommissionDistributionVO>> page(@Valid UnionCommissionDistributionQuery query) {
        return Result.success(commissionDistributionService.getCommissionDistributionPage(query));
    }

}
