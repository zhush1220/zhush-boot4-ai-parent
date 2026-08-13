package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionAdjustmentParam;
import com.zhush.union.query.UnionCommissionAdjustmentQuery;
import com.zhush.union.service.UnionCommissionAdjustmentService;
import com.zhush.union.vo.UnionCommissionAdjustmentVO;
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
 * @ClassName UnionCommissionAdjustmentController
 * @Description 佣金调整记录表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-佣金调整记录", description = "佣金调整记录的增删改查")
@RequestMapping("/admin/union/commission-adjustment")
public class UnionCommissionAdjustmentController {

    private final UnionCommissionAdjustmentService commissionAdjustmentService;

    public UnionCommissionAdjustmentController(UnionCommissionAdjustmentService commissionAdjustmentService) {
        this.commissionAdjustmentService = commissionAdjustmentService;
    }

    /**
     * 新增佣金调整记录
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:commission-adjustment:create")
    @OperateLog(module = "佣金调整记录", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增佣金调整记录")
    public Result<Long> create(@RequestBody @Valid UnionCommissionAdjustmentParam param) {
        return Result.success(commissionAdjustmentService.createCommissionAdjustment(param));
    }

    /**
     * 修改佣金调整记录
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:commission-adjustment:update")
    @OperateLog(module = "佣金调整记录", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改佣金调整记录")
    public Result<Void> update(@RequestBody @Valid UnionCommissionAdjustmentParam param) {
        commissionAdjustmentService.updateCommissionAdjustment(param);
        return Result.success();
    }

    /**
     * 删除佣金调整记录
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:commission-adjustment:delete")
    @OperateLog(module = "佣金调整记录", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除佣金调整记录")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        commissionAdjustmentService.deleteCommissionAdjustment(id);
        return Result.success();
    }

    /**
     * 佣金调整记录详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:commission-adjustment:query")
    @Operation(summary = "佣金调整记录详情")
    public Result<UnionCommissionAdjustmentVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(commissionAdjustmentService.getCommissionAdjustment(id));
    }

    /**
     * 佣金调整记录分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:commission-adjustment:query")
    @Operation(summary = "佣金调整记录分页")
    public Result<PageResult<UnionCommissionAdjustmentVO>> page(@Valid UnionCommissionAdjustmentQuery query) {
        return Result.success(commissionAdjustmentService.getCommissionAdjustmentPage(query));
    }

}
