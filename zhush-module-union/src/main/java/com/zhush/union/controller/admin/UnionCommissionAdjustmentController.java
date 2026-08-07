package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionAdjustmentParam;
import com.zhush.union.query.UnionCommissionAdjustmentQuery;
import com.zhush.union.service.UnionCommissionAdjustmentService;
import com.zhush.union.vo.UnionCommissionAdjustmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionCommissionAdjustmentController
 * @Description 佣金调整记录表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-佣金调整记录表")
@RequestMapping("/admin/union/commission-adjustment")
public class UnionCommissionAdjustmentController {

    private final UnionCommissionAdjustmentService commissionAdjustmentService;

    public UnionCommissionAdjustmentController(UnionCommissionAdjustmentService commissionAdjustmentService) {
        this.commissionAdjustmentService = commissionAdjustmentService;
    }

}
