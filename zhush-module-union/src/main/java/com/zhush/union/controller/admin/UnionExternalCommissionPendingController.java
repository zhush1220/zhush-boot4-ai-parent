package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalCommissionPendingParam;
import com.zhush.union.query.UnionExternalCommissionPendingQuery;
import com.zhush.union.service.UnionExternalCommissionPendingService;
import com.zhush.union.vo.UnionExternalCommissionPendingVO;
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
 * @ClassName UnionExternalCommissionPendingController
 * @Description 外部推广者待结算佣金表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者待结算佣金", description = "外部推广者待结算佣金的增删改查")
@RequestMapping("/admin/union/external-commission-pending")
public class UnionExternalCommissionPendingController {

    private final UnionExternalCommissionPendingService externalCommissionPendingService;

    public UnionExternalCommissionPendingController(UnionExternalCommissionPendingService externalCommissionPendingService) {
        this.externalCommissionPendingService = externalCommissionPendingService;
    }

    /**
     * 新增外部推广者待结算佣金
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-pending:create")
    @OperateLog(module = "外部推广者待结算佣金", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者待结算佣金")
    public Result<Long> create(@RequestBody @Valid UnionExternalCommissionPendingParam param) {
        return Result.success(externalCommissionPendingService.createExternalCommissionPending(param));
    }

    /**
     * 修改外部推广者待结算佣金
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-pending:update")
    @OperateLog(module = "外部推广者待结算佣金", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者待结算佣金")
    public Result<Void> update(@RequestBody @Valid UnionExternalCommissionPendingParam param) {
        externalCommissionPendingService.updateExternalCommissionPending(param);
        return Result.success();
    }

    /**
     * 删除外部推广者待结算佣金
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-pending:delete")
    @OperateLog(module = "外部推广者待结算佣金", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者待结算佣金")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalCommissionPendingService.deleteExternalCommissionPending(id);
        return Result.success();
    }

    /**
     * 外部推广者待结算佣金详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-pending:query")
    @Operation(summary = "外部推广者待结算佣金详情")
    public Result<UnionExternalCommissionPendingVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalCommissionPendingService.getExternalCommissionPending(id));
    }

    /**
     * 外部推广者待结算佣金分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-pending:query")
    @Operation(summary = "外部推广者待结算佣金分页")
    public Result<PageResult<UnionExternalCommissionPendingVO>> page(@Valid UnionExternalCommissionPendingQuery query) {
        return Result.success(externalCommissionPendingService.getExternalCommissionPendingPage(query));
    }

}
