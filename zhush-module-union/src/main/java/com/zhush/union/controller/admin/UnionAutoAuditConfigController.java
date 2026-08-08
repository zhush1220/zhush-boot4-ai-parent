package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionAutoAuditConfigParam;
import com.zhush.union.query.UnionAutoAuditConfigQuery;
import com.zhush.union.service.UnionAutoAuditConfigService;
import com.zhush.union.vo.UnionAutoAuditConfigVO;
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
 * @ClassName UnionAutoAuditConfigController
 * @Description 自动审核配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-自动审核配置", description = "自动审核配置的增删改查")
@RequestMapping("/admin/union/auto-audit-config")
public class UnionAutoAuditConfigController {

    private final UnionAutoAuditConfigService autoAuditConfigService;

    public UnionAutoAuditConfigController(UnionAutoAuditConfigService autoAuditConfigService) {
        this.autoAuditConfigService = autoAuditConfigService;
    }

    /**
     * 新增自动审核配置
     */
    @PostMapping
    @OperateLog(module = "自动审核配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增自动审核配置")
    public Result<Long> create(@RequestBody @Valid UnionAutoAuditConfigParam param) {
        return Result.success(autoAuditConfigService.createAutoAuditConfig(param));
    }

    /**
     * 修改自动审核配置
     */
    @PutMapping
    @OperateLog(module = "自动审核配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改自动审核配置")
    public Result<Void> update(@RequestBody @Valid UnionAutoAuditConfigParam param) {
        autoAuditConfigService.updateAutoAuditConfig(param);
        return Result.success();
    }

    /**
     * 删除自动审核配置
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "自动审核配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除自动审核配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        autoAuditConfigService.deleteAutoAuditConfig(id);
        return Result.success();
    }

    /**
     * 自动审核配置详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "自动审核配置详情")
    public Result<UnionAutoAuditConfigVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(autoAuditConfigService.getAutoAuditConfig(id));
    }

    /**
     * 自动审核配置分页
     */
    @GetMapping("/page")
    @Operation(summary = "自动审核配置分页")
    public Result<PageResult<UnionAutoAuditConfigVO>> page(@Valid UnionAutoAuditConfigQuery query) {
        return Result.success(autoAuditConfigService.getAutoAuditConfigPage(query));
    }

}
