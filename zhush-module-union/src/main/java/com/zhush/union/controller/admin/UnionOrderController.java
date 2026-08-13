package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionOrderParam;
import com.zhush.union.query.UnionOrderQuery;
import com.zhush.union.service.UnionOrderService;
import com.zhush.union.vo.UnionOrderVO;
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
 * @ClassName UnionOrderController
 * @Description 联盟订单主表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-联盟订单主", description = "联盟订单主的增删改查")
@RequestMapping("/admin/union/order")
public class UnionOrderController {

    private final UnionOrderService orderService;

    public UnionOrderController(UnionOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 新增联盟订单主
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order:create")
    @OperateLog(module = "联盟订单主", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增联盟订单主")
    public Result<Long> create(@RequestBody @Valid UnionOrderParam param) {
        return Result.success(orderService.createOrder(param));
    }

    /**
     * 修改联盟订单主
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order:update")
    @OperateLog(module = "联盟订单主", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改联盟订单主")
    public Result<Void> update(@RequestBody @Valid UnionOrderParam param) {
        orderService.updateOrder(param);
        return Result.success();
    }

    /**
     * 删除联盟订单主
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order:delete")
    @OperateLog(module = "联盟订单主", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除联盟订单主")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }

    /**
     * 联盟订单主详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order:query")
    @Operation(summary = "联盟订单主详情")
    public Result<UnionOrderVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(orderService.getOrder(id));
    }

    /**
     * 联盟订单主分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:order:query")
    @Operation(summary = "联盟订单主分页")
    public Result<PageResult<UnionOrderVO>> page(@Valid UnionOrderQuery query) {
        return Result.success(orderService.getOrderPage(query));
    }

}
