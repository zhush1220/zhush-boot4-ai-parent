package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.ai.param.AiModelParam;
import com.zhush.ai.query.AiModelQuery;
import com.zhush.ai.service.AiModelService;
import com.zhush.ai.vo.AiModelVO;
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

import java.util.List;

/**
 * 管理端-AI 模型配置管理接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI 模型配置", description = "模型 CRUD / 启停 / 连接测试")
@RequestMapping("/admin/ai/model")
public class AiModelController {

    private final AiModelService modelService;

    public AiModelController(AiModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/page")
    @Operation(summary = "模型分页")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:list")
    public Result<PageResult<AiModelVO>> page(@Valid AiModelQuery query) {
        return Result.success(modelService.page(query));
    }

    @GetMapping("/list")
    @Operation(summary = "启用态模型列表（下拉）")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "agent:definition:list")
    public Result<List<AiModelVO>> list() {
        return Result.success(modelService.listEnabled());
    }

    @GetMapping("/{id}")
    @Operation(summary = "模型详情")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:list")
    public Result<AiModelVO> get(@PathVariable Long id) {
        return Result.success(modelService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增模型")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:create")
    public Result<Long> create(@RequestBody @Valid AiModelParam param) {
        return Result.success(modelService.create(param));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑模型")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid AiModelParam param) {
        param.setId(id);
        modelService.update(param);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:delete")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "模型启停")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:update")
    public Result<Void> toggle(@PathVariable Long id) {
        modelService.toggle(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "模型连接测试")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:model:update")
    public Result<Void> test(@PathVariable Long id) {
        modelService.test(id);
        return Result.success();
    }
}
