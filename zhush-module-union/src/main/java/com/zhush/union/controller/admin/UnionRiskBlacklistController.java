package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskBlacklistParam;
import com.zhush.union.query.UnionRiskBlacklistQuery;
import com.zhush.union.service.UnionRiskBlacklistService;
import com.zhush.union.vo.UnionRiskBlacklistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionRiskBlacklistController
 * @Description 风控黑名单表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-风控黑名单表")
@RequestMapping("/admin/union/risk-blacklist")
public class UnionRiskBlacklistController {

    private final UnionRiskBlacklistService riskBlacklistService;

    public UnionRiskBlacklistController(UnionRiskBlacklistService riskBlacklistService) {
        this.riskBlacklistService = riskBlacklistService;
    }

}
