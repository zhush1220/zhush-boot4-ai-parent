package com.zhush.ai.controller.admin;

import com.zhush.ai.query.AgentAuditLogQuery;
import com.zhush.ai.service.AgentLogService;
import com.zhush.ai.vo.AgentAuditLogVO;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-AI 审计日志只读接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI审计日志", description = "审计日志分页与详情（只读）")
@RequestMapping("/admin/ai/agent/log")
public class AgentLogController {

    private final AgentLogService logService;

    public AgentLogController(AgentLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/page")
    @Operation(summary = "审计日志分页")
    public Result<PageResult<AgentAuditLogVO>> page(@Valid AgentAuditLogQuery query) {
        return Result.success(logService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审计日志详情")
    public Result<AgentAuditLogVO> get(@PathVariable Long id) {
        return Result.success(logService.get(id));
    }
}
