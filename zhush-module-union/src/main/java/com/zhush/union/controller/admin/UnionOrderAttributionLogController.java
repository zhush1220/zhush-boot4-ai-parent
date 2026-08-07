package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionOrderAttributionLogParam;
import com.zhush.union.query.UnionOrderAttributionLogQuery;
import com.zhush.union.service.UnionOrderAttributionLogService;
import com.zhush.union.vo.UnionOrderAttributionLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionOrderAttributionLogController
 * @Description 订单归因日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-订单归因日志表")
@RequestMapping("/admin/union/order-attribution-log")
public class UnionOrderAttributionLogController {

    private final UnionOrderAttributionLogService orderAttributionLogService;

    public UnionOrderAttributionLogController(UnionOrderAttributionLogService orderAttributionLogService) {
        this.orderAttributionLogService = orderAttributionLogService;
    }

}
