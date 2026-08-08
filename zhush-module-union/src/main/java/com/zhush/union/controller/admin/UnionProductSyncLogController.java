package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductSyncLogParam;
import com.zhush.union.query.UnionProductSyncLogQuery;
import com.zhush.union.service.UnionProductSyncLogService;
import com.zhush.union.vo.UnionProductSyncLogVO;
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
 * @ClassName UnionProductSyncLogController
 * @Description 商品同步日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-商品同步日志", description = "商品同步日志的增删改查")
@RequestMapping("/admin/union/product-sync-log")
public class UnionProductSyncLogController {

    private final UnionProductSyncLogService productSyncLogService;

    public UnionProductSyncLogController(UnionProductSyncLogService productSyncLogService) {
        this.productSyncLogService = productSyncLogService;
    }

    /**
     * 新增商品同步日志
     */
    @PostMapping
    @OperateLog(module = "商品同步日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增商品同步日志")
    public Result<Long> create(@RequestBody @Valid UnionProductSyncLogParam param) {
        return Result.success(productSyncLogService.createProductSyncLog(param));
    }

    /**
     * 修改商品同步日志
     */
    @PutMapping
    @OperateLog(module = "商品同步日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改商品同步日志")
    public Result<Void> update(@RequestBody @Valid UnionProductSyncLogParam param) {
        productSyncLogService.updateProductSyncLog(param);
        return Result.success();
    }

    /**
     * 删除商品同步日志
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "商品同步日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除商品同步日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        productSyncLogService.deleteProductSyncLog(id);
        return Result.success();
    }

    /**
     * 商品同步日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品同步日志详情")
    public Result<UnionProductSyncLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(productSyncLogService.getProductSyncLog(id));
    }

    /**
     * 商品同步日志分页
     */
    @GetMapping("/page")
    @Operation(summary = "商品同步日志分页")
    public Result<PageResult<UnionProductSyncLogVO>> page(@Valid UnionProductSyncLogQuery query) {
        return Result.success(productSyncLogService.getProductSyncLogPage(query));
    }

}
