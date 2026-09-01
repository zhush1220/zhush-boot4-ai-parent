package com.zhush.ai.controller.admin;

import com.zhush.ai.param.AgentApprovalParam;
import com.zhush.ai.query.AgentApprovalQuery;
import com.zhush.ai.service.AgentApprovalService;
import com.zhush.ai.vo.AgentApprovalVO;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-智能体敏感工具审批接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-智能体审批", description = "敏感工具人工审批（通过/改参/驳回）")
@RequestMapping("/admin/ai/agent/approval")
public class AgentApprovalController {

    private final AgentApprovalService approvalService;

    public AgentApprovalController(AgentApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/page")
    @Operation(summary = "审批任务分页")
    public Result<PageResult<AgentApprovalVO>> page(@Valid AgentApprovalQuery query) {
        return Result.success(approvalService.page(query));
    }

    @PostMapping("/approve")
    @Operation(summary = "通过（可改参）")
    public Result<Void> approve(@RequestBody @Valid AgentApprovalParam param) {
        approvalService.approve(param.getApprovalId(), param.getModifiedArgs());
        return Result.success();
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回")
    public Result<Void> reject(@RequestBody @Valid AgentApprovalParam param) {
        approvalService.reject(param.getApprovalId(), param.getReason());
        return Result.success();
    }
}
