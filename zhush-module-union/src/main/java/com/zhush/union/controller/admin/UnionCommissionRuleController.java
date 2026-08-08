package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionRuleParam;
import com.zhush.union.query.UnionCommissionRuleQuery;
import com.zhush.union.service.UnionCommissionRuleService;
import com.zhush.union.vo.UnionCommissionRuleVO;
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
 * @ClassName UnionCommissionRuleController
 * @Description 佣金分配规则配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-佣金分配规则配置", description = "佣金分配规则配置的增删改查")
@RequestMapping("/admin/union/commission-rule")
public class UnionCommissionRuleController {

    private final UnionCommissionRuleService commissionRuleService;

    public UnionCommissionRuleController(UnionCommissionRuleService commissionRuleService) {
        this.commissionRuleService = commissionRuleService;
    }

    /**
     * 新增佣金分配规则配置
     */
    @PostMapping
    @OperateLog(module = "佣金分配规则配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增佣金分配规则配置")
    public Result<Long> create(@RequestBody @Valid UnionCommissionRuleParam param) {
        return Result.success(commissionRuleService.createCommissionRule(param));
    }

    /**
     * 修改佣金分配规则配置
     */
    @PutMapping
    @OperateLog(module = "佣金分配规则配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改佣金分配规则配置")
    public Result<Void> update(@RequestBody @Valid UnionCommissionRuleParam param) {
        commissionRuleService.updateCommissionRule(param);
        return Result.success();
    }

    /**
     * 删除佣金分配规则配置
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "佣金分配规则配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除佣金分配规则配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        commissionRuleService.deleteCommissionRule(id);
        return Result.success();
    }

    /**
     * 佣金分配规则配置详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "佣金分配规则配置详情")
    public Result<UnionCommissionRuleVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(commissionRuleService.getCommissionRule(id));
    }

    /**
     * 佣金分配规则配置分页
     */
    @GetMapping("/page")
    @Operation(summary = "佣金分配规则配置分页")
    public Result<PageResult<UnionCommissionRuleVO>> page(@Valid UnionCommissionRuleQuery query) {
        return Result.success(commissionRuleService.getCommissionRulePage(query));
    }

}
