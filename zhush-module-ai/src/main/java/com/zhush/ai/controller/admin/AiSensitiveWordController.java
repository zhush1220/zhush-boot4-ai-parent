package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.ai.param.AiSensitiveWordParam;
import com.zhush.ai.query.AiSensitiveWordQuery;
import com.zhush.ai.service.AiSensitiveWordService;
import com.zhush.ai.vo.AiSensitiveWordVO;
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
 * 管理端-AI 敏感词库管理接口。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI 敏感词库", description = "敏感词 CRUD / 启停（审批门禁动态词库）")
@RequestMapping("/admin/ai/sensitive")
public class AiSensitiveWordController {

    private final AiSensitiveWordService wordService;

    public AiSensitiveWordController(AiSensitiveWordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping("/page")
    @Operation(summary = "敏感词分页")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:list")
    public Result<PageResult<AiSensitiveWordVO>> page(@Valid AiSensitiveWordQuery query) {
        return Result.success(wordService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "敏感词详情")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:list")
    public Result<AiSensitiveWordVO> get(@PathVariable Long id) {
        return Result.success(wordService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增敏感词")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:create")
    public Result<Long> create(@RequestBody @Valid AiSensitiveWordParam param) {
        return Result.success(wordService.create(param));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑敏感词")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid AiSensitiveWordParam param) {
        param.setId(id);
        wordService.update(param);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除敏感词")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:delete")
    public Result<Void> delete(@PathVariable Long id) {
        wordService.delete(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除敏感词")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:delete")
    public Result<Void> deleteBatch(@io.swagger.v3.oas.annotations.Parameter(description = "敏感词ID列表") @RequestBody List<Long> ids) {
        wordService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "敏感词启停")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:sensitive:update")
    public Result<Void> toggle(@PathVariable Long id) {
        wordService.toggle(id);
        return Result.success();
    }
}
