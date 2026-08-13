package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionOrderAttributionLogParam;
import com.zhush.union.query.UnionOrderAttributionLogQuery;
import com.zhush.union.service.UnionOrderAttributionLogService;
import com.zhush.union.vo.UnionOrderAttributionLogVO;
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
 * @ClassName UnionOrderAttributionLogController
 * @Description 订单归因日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-订单归因日志", description = "订单归因日志的增删改查")
@RequestMapping("/admin/union/order-attribution-log")
public class UnionOrderAttributionLogController {

    private final UnionOrderAttributionLogService orderAttributionLogService;

    public UnionOrderAttributionLogController(UnionOrderAttributionLogService orderAttributionLogService) {
        this.orderAttributionLogService = orderAttributionLogService;
    }

    /**
     * 新增订单归因日志
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order-attribution-log:create")
    @OperateLog(module = "订单归因日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增订单归因日志")
    public Result<Long> create(@RequestBody @Valid UnionOrderAttributionLogParam param) {
        return Result.success(orderAttributionLogService.createOrderAttributionLog(param));
    }

    /**
     * 修改订单归因日志
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order-attribution-log:update")
    @OperateLog(module = "订单归因日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改订单归因日志")
    public Result<Void> update(@RequestBody @Valid UnionOrderAttributionLogParam param) {
        orderAttributionLogService.updateOrderAttributionLog(param);
        return Result.success();
    }

    /**
     * 删除订单归因日志
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order-attribution-log:delete")
    @OperateLog(module = "订单归因日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除订单归因日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        orderAttributionLogService.deleteOrderAttributionLog(id);
        return Result.success();
    }

    /**
     * 订单归因日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order-attribution-log:query")
    @Operation(summary = "订单归因日志详情")
    public Result<UnionOrderAttributionLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(orderAttributionLogService.getOrderAttributionLog(id));
    }

    /**
     * 订单归因日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order-attribution-log:query")
    @Operation(summary = "订单归因日志分页")
    public Result<PageResult<UnionOrderAttributionLogVO>> page(@Valid UnionOrderAttributionLogQuery query) {
        return Result.success(orderAttributionLogService.getOrderAttributionLogPage(query));
    }

}
