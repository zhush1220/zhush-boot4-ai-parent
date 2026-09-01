package com.zhush.ai.controller.admin;

import com.zhush.ai.param.AgentDefinitionParam;
import com.zhush.ai.query.AgentDefinitionQuery;
import com.zhush.ai.service.AgentManageService;
import com.zhush.ai.vo.AgentDefinitionVO;
import com.zhush.ai.vo.AgentModelVO;
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

import java.util.List;

/**
 * 管理端-智能体定义管理接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-智能体定义", description = "Agent 定义 CRUD 与模型值域")
@RequestMapping("/admin/ai/agent/manage")
public class AgentDefinitionController {

    private final AgentManageService manageService;

    public AgentDefinitionController(AgentManageService manageService) {
        this.manageService = manageService;
    }

    @GetMapping("/page")
    @Operation(summary = "Agent 定义分页")
    public Result<PageResult<AgentDefinitionVO>> page(@Valid AgentDefinitionQuery query) {
        return Result.success(manageService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Agent 定义详情")
    public Result<AgentDefinitionVO> get(@PathVariable Long id) {
        return Result.success(manageService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增 Agent 定义")
    public Result<Long> create(@RequestBody @Valid AgentDefinitionParam param) {
        return Result.success(manageService.create(param));
    }

    @PutMapping
    @Operation(summary = "编辑 Agent 定义")
    public Result<Void> update(@RequestBody @Valid AgentDefinitionParam param) {
        manageService.update(param);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Agent 定义")
    public Result<Void> delete(@PathVariable Long id) {
        manageService.delete(id);
        return Result.success();
    }

    @GetMapping("/models")
    @Operation(summary = "可用模型列表")
    public Result<List<AgentModelVO>> models() {
        return Result.success(manageService.models());
    }
}
