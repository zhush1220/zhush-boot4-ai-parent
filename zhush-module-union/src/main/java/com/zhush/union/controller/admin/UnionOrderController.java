package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionOrderParam;
import com.zhush.union.query.UnionOrderQuery;
import com.zhush.union.service.UnionOrderService;
import com.zhush.union.vo.UnionOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionOrderController
 * @Description 联盟订单主表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-联盟订单主表")
@RequestMapping("/admin/union/order")
public class UnionOrderController {

    private final UnionOrderService orderService;

    public UnionOrderController(UnionOrderService orderService) {
        this.orderService = orderService;
    }

}
