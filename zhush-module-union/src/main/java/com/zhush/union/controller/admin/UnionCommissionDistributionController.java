package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionDistributionParam;
import com.zhush.union.query.UnionCommissionDistributionQuery;
import com.zhush.union.service.UnionCommissionDistributionService;
import com.zhush.union.vo.UnionCommissionDistributionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionCommissionDistributionController
 * @Description 佣金分配明细表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-佣金分配明细表")
@RequestMapping("/admin/union/commission-distribution")
public class UnionCommissionDistributionController {

    private final UnionCommissionDistributionService commissionDistributionService;

    public UnionCommissionDistributionController(UnionCommissionDistributionService commissionDistributionService) {
        this.commissionDistributionService = commissionDistributionService;
    }

}
