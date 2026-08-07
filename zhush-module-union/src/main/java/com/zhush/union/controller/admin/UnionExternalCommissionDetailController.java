package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalCommissionDetailParam;
import com.zhush.union.query.UnionExternalCommissionDetailQuery;
import com.zhush.union.service.UnionExternalCommissionDetailService;
import com.zhush.union.vo.UnionExternalCommissionDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalCommissionDetailController
 * @Description 外部推广者佣金明细表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者佣金明细表")
@RequestMapping("/admin/union/external-commission-detail")
public class UnionExternalCommissionDetailController {

    private final UnionExternalCommissionDetailService externalCommissionDetailService;

    public UnionExternalCommissionDetailController(UnionExternalCommissionDetailService externalCommissionDetailService) {
        this.externalCommissionDetailService = externalCommissionDetailService;
    }

}
