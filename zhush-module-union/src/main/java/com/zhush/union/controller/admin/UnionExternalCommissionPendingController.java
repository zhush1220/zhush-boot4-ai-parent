package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalCommissionPendingParam;
import com.zhush.union.query.UnionExternalCommissionPendingQuery;
import com.zhush.union.service.UnionExternalCommissionPendingService;
import com.zhush.union.vo.UnionExternalCommissionPendingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalCommissionPendingController
 * @Description 外部推广者待结算佣金表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者待结算佣金表")
@RequestMapping("/admin/union/external-commission-pending")
public class UnionExternalCommissionPendingController {

    private final UnionExternalCommissionPendingService externalCommissionPendingService;

    public UnionExternalCommissionPendingController(UnionExternalCommissionPendingService externalCommissionPendingService) {
        this.externalCommissionPendingService = externalCommissionPendingService;
    }

}
