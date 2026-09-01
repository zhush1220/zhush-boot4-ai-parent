package com.zhush.ai.controller.admin;

import com.zhush.ai.service.AgentToolService;
import com.zhush.ai.vo.AgentToolVO;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-已注册工具只读接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-智能体工具", description = "已注册工具列表（只读）")
@RequestMapping("/admin/ai/agent/tool")
public class AgentToolController {

    private final AgentToolService toolService;

    public AgentToolController(AgentToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping("/list")
    @Operation(summary = "已注册工具列表")
    public Result<List<AgentToolVO>> list() {
        return Result.success(toolService.list());
    }
}
