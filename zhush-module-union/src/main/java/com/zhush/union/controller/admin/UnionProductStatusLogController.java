package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductStatusLogParam;
import com.zhush.union.query.UnionProductStatusLogQuery;
import com.zhush.union.service.UnionProductStatusLogService;
import com.zhush.union.vo.UnionProductStatusLogVO;
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
 * @ClassName UnionProductStatusLogController
 * @Description 商品状态变更日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-商品状态变更日志", description = "商品状态变更日志的增删改查")
@RequestMapping("/admin/union/product-status-log")
public class UnionProductStatusLogController {

    private final UnionProductStatusLogService productStatusLogService;

    public UnionProductStatusLogController(UnionProductStatusLogService productStatusLogService) {
        this.productStatusLogService = productStatusLogService;
    }

    /**
     * 新增商品状态变更日志
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:product-status-log:create")
    @OperateLog(module = "商品状态变更日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增商品状态变更日志")
    public Result<Long> create(@RequestBody @Valid UnionProductStatusLogParam param) {
        return Result.success(productStatusLogService.createProductStatusLog(param));
    }

    /**
     * 修改商品状态变更日志
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:product-status-log:update")
    @OperateLog(module = "商品状态变更日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改商品状态变更日志")
    public Result<Void> update(@RequestBody @Valid UnionProductStatusLogParam param) {
        productStatusLogService.updateProductStatusLog(param);
        return Result.success();
    }

    /**
     * 删除商品状态变更日志
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:product-status-log:delete")
    @OperateLog(module = "商品状态变更日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除商品状态变更日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        productStatusLogService.deleteProductStatusLog(id);
        return Result.success();
    }

    /**
     * 商品状态变更日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:product-status-log:query")
    @Operation(summary = "商品状态变更日志详情")
    public Result<UnionProductStatusLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(productStatusLogService.getProductStatusLog(id));
    }

    /**
     * 商品状态变更日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:product-status-log:query")
    @Operation(summary = "商品状态变更日志分页")
    public Result<PageResult<UnionProductStatusLogVO>> page(@Valid UnionProductStatusLogQuery query) {
        return Result.success(productStatusLogService.getProductStatusLogPage(query));
    }

}
