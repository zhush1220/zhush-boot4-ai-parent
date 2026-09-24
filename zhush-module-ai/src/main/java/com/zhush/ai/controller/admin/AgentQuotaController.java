package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.ai.param.AiQuotaRuleParam;
import com.zhush.ai.query.AiQuotaRuleQuery;
import com.zhush.ai.service.AiQuotaRuleService;
import com.zhush.ai.vo.AiQuotaRuleVO;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
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
 * 管理端-AI 配额规则管理接口（菜单 3117，权限 agent:quota:*）。
 *
 * <p>规则实时生效：判定侧 {@code AiQuotaService} 每次直查 DB，无缓存需刷新。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI 配额规则", description = "按用户类型+任务类型限制日/月调用次数（0=不限）")
@RequestMapping("/admin/ai/quota")
public class AgentQuotaController {

    private final AiQuotaRuleService quotaRuleService;

    public AgentQuotaController(AiQuotaRuleService quotaRuleService) {
        this.quotaRuleService = quotaRuleService;
    }

    @GetMapping("/page")
    @Operation(summary = "配额规则分页")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:list")
    public Result<PageResult<AiQuotaRuleVO>> page(@Valid AiQuotaRuleQuery query) {
        return Result.success(quotaRuleService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "配额规则详情")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:list")
    public Result<AiQuotaRuleVO> get(@PathVariable Long id) {
        return Result.success(quotaRuleService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增配额规则")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:create")
    public Result<Long> create(@RequestBody @Valid AiQuotaRuleParam param) {
        return Result.success(quotaRuleService.create(param));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑配额规则")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid AiQuotaRuleParam param) {
        param.setId(id);
        quotaRuleService.update(param);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配额规则")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:delete")
    public Result<Void> delete(@PathVariable Long id) {
        quotaRuleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "配额规则启停")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:quota:update")
    public Result<Void> toggle(@PathVariable Long id) {
        quotaRuleService.toggle(id);
        return Result.success();
    }
}
