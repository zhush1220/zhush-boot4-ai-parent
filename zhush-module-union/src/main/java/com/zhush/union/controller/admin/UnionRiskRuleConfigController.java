package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskRuleConfigParam;
import com.zhush.union.query.UnionRiskRuleConfigQuery;
import com.zhush.union.service.UnionRiskRuleConfigService;
import com.zhush.union.vo.UnionRiskRuleConfigVO;
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
 * @ClassName UnionRiskRuleConfigController
 * @Description 风控规则配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-风控规则配置", description = "风控规则配置的增删改查")
@RequestMapping("/admin/union/risk-rule-config")
public class UnionRiskRuleConfigController {

    private final UnionRiskRuleConfigService riskRuleConfigService;

    public UnionRiskRuleConfigController(UnionRiskRuleConfigService riskRuleConfigService) {
        this.riskRuleConfigService = riskRuleConfigService;
    }

    /**
     * 新增风控规则配置
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:risk-rule-config:create")
    @OperateLog(module = "风控规则配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增风控规则配置")
    public Result<Long> create(@RequestBody @Valid UnionRiskRuleConfigParam param) {
        return Result.success(riskRuleConfigService.createRiskRuleConfig(param));
    }

    /**
     * 修改风控规则配置
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:risk-rule-config:update")
    @OperateLog(module = "风控规则配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改风控规则配置")
    public Result<Void> update(@RequestBody @Valid UnionRiskRuleConfigParam param) {
        riskRuleConfigService.updateRiskRuleConfig(param);
        return Result.success();
    }

    /**
     * 删除风控规则配置
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:risk-rule-config:delete")
    @OperateLog(module = "风控规则配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除风控规则配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        riskRuleConfigService.deleteRiskRuleConfig(id);
        return Result.success();
    }

    /**
     * 风控规则配置详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:risk-rule-config:query")
    @Operation(summary = "风控规则配置详情")
    public Result<UnionRiskRuleConfigVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(riskRuleConfigService.getRiskRuleConfig(id));
    }

    /**
     * 风控规则配置分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:risk-rule-config:query")
    @Operation(summary = "风控规则配置分页")
    public Result<PageResult<UnionRiskRuleConfigVO>> page(@Valid UnionRiskRuleConfigQuery query) {
        return Result.success(riskRuleConfigService.getRiskRuleConfigPage(query));
    }

}
