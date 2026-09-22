package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.ai.query.AgentUsageQuery;
import com.zhush.ai.service.AgentUsageService;
import com.zhush.ai.vo.AgentUsageSummaryVO;
import com.zhush.ai.vo.AgentUsageTrendVO;
import com.zhush.ai.vo.AgentUsageVO;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-AI 用量统计接口（只读报表）。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI 用量统计", description = "Token 用量与成本报表（只读）")
@RequestMapping("/admin/ai/agent/usage")
public class AgentUsageController {

    private final AgentUsageService usageService;

    public AgentUsageController(AgentUsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping("/page")
    @Operation(summary = "用量汇总分页")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:usage:list")
    public Result<PageResult<AgentUsageVO>> page(@Valid AgentUsageQuery query) {
        return Result.success(usageService.page(query));
    }

    @GetMapping("/summary")
    @Operation(summary = "用量汇总合计（与分页同筛选条件）")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:usage:list")
    public Result<AgentUsageSummaryVO> summary(@Valid AgentUsageQuery query) {
        return Result.success(usageService.summary(query));
    }

    @GetMapping("/trend")
    @Operation(summary = "每日用量趋势（与分页同筛选条件）")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:usage:list")
    public Result<List<AgentUsageTrendVO>> trend(@Valid AgentUsageQuery query) {
        return Result.success(usageService.trend(query));
    }
}
