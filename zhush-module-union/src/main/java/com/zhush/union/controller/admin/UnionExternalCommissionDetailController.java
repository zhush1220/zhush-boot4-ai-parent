package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalCommissionDetailParam;
import com.zhush.union.query.UnionExternalCommissionDetailQuery;
import com.zhush.union.service.UnionExternalCommissionDetailService;
import com.zhush.union.vo.UnionExternalCommissionDetailVO;
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
 * @ClassName UnionExternalCommissionDetailController
 * @Description 外部推广者佣金明细表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者佣金明细", description = "外部推广者佣金明细的增删改查")
@RequestMapping("/admin/union/external-commission-detail")
public class UnionExternalCommissionDetailController {

    private final UnionExternalCommissionDetailService externalCommissionDetailService;

    public UnionExternalCommissionDetailController(UnionExternalCommissionDetailService externalCommissionDetailService) {
        this.externalCommissionDetailService = externalCommissionDetailService;
    }

    /**
     * 新增外部推广者佣金明细
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-detail:create")
    @OperateLog(module = "外部推广者佣金明细", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者佣金明细")
    public Result<Long> create(@RequestBody @Valid UnionExternalCommissionDetailParam param) {
        return Result.success(externalCommissionDetailService.createExternalCommissionDetail(param));
    }

    /**
     * 修改外部推广者佣金明细
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-detail:update")
    @OperateLog(module = "外部推广者佣金明细", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者佣金明细")
    public Result<Void> update(@RequestBody @Valid UnionExternalCommissionDetailParam param) {
        externalCommissionDetailService.updateExternalCommissionDetail(param);
        return Result.success();
    }

    /**
     * 删除外部推广者佣金明细
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-detail:delete")
    @OperateLog(module = "外部推广者佣金明细", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者佣金明细")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalCommissionDetailService.deleteExternalCommissionDetail(id);
        return Result.success();
    }

    /**
     * 外部推广者佣金明细详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-detail:query")
    @Operation(summary = "外部推广者佣金明细详情")
    public Result<UnionExternalCommissionDetailVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalCommissionDetailService.getExternalCommissionDetail(id));
    }

    /**
     * 外部推广者佣金明细分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-commission-detail:query")
    @Operation(summary = "外部推广者佣金明细分页")
    public Result<PageResult<UnionExternalCommissionDetailVO>> page(@Valid UnionExternalCommissionDetailQuery query) {
        return Result.success(externalCommissionDetailService.getExternalCommissionDetailPage(query));
    }

}
