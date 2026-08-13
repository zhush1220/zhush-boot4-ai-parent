package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLevelConfigParam;
import com.zhush.union.query.UnionLevelConfigQuery;
import com.zhush.union.service.UnionLevelConfigService;
import com.zhush.union.vo.UnionLevelConfigVO;
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
 * @ClassName UnionLevelConfigController
 * @Description 层级佣金配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-层级佣金配置", description = "层级佣金配置的增删改查")
@RequestMapping("/admin/union/level-config")
public class UnionLevelConfigController {

    private final UnionLevelConfigService levelConfigService;

    public UnionLevelConfigController(UnionLevelConfigService levelConfigService) {
        this.levelConfigService = levelConfigService;
    }

    /**
     * 新增层级佣金配置
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:level-config:create")
    @OperateLog(module = "层级佣金配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增层级佣金配置")
    public Result<Long> create(@RequestBody @Valid UnionLevelConfigParam param) {
        return Result.success(levelConfigService.createLevelConfig(param));
    }

    /**
     * 修改层级佣金配置
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:level-config:update")
    @OperateLog(module = "层级佣金配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改层级佣金配置")
    public Result<Void> update(@RequestBody @Valid UnionLevelConfigParam param) {
        levelConfigService.updateLevelConfig(param);
        return Result.success();
    }

    /**
     * 删除层级佣金配置
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:level-config:delete")
    @OperateLog(module = "层级佣金配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除层级佣金配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        levelConfigService.deleteLevelConfig(id);
        return Result.success();
    }

    /**
     * 层级佣金配置详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:level-config:query")
    @Operation(summary = "层级佣金配置详情")
    public Result<UnionLevelConfigVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(levelConfigService.getLevelConfig(id));
    }

    /**
     * 层级佣金配置分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:level-config:query")
    @Operation(summary = "层级佣金配置分页")
    public Result<PageResult<UnionLevelConfigVO>> page(@Valid UnionLevelConfigQuery query) {
        return Result.success(levelConfigService.getLevelConfigPage(query));
    }

}
