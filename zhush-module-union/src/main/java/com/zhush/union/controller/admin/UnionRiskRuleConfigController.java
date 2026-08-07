package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskRuleConfigParam;
import com.zhush.union.query.UnionRiskRuleConfigQuery;
import com.zhush.union.service.UnionRiskRuleConfigService;
import com.zhush.union.vo.UnionRiskRuleConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionRiskRuleConfigController
 * @Description 风控规则配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-风控规则配置表")
@RequestMapping("/admin/union/risk-rule-config")
public class UnionRiskRuleConfigController {

    private final UnionRiskRuleConfigService riskRuleConfigService;

    public UnionRiskRuleConfigController(UnionRiskRuleConfigService riskRuleConfigService) {
        this.riskRuleConfigService = riskRuleConfigService;
    }

}
