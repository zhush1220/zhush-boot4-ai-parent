package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionRuleParam;
import com.zhush.union.query.UnionCommissionRuleQuery;
import com.zhush.union.service.UnionCommissionRuleService;
import com.zhush.union.vo.UnionCommissionRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionCommissionRuleController
 * @Description 佣金分配规则配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-佣金分配规则配置表")
@RequestMapping("/admin/union/commission-rule")
public class UnionCommissionRuleController {

    private final UnionCommissionRuleService commissionRuleService;

    public UnionCommissionRuleController(UnionCommissionRuleService commissionRuleService) {
        this.commissionRuleService = commissionRuleService;
    }

}
