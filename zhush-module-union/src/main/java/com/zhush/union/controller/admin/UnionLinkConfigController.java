package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLinkConfigParam;
import com.zhush.union.query.UnionLinkConfigQuery;
import com.zhush.union.service.UnionLinkConfigService;
import com.zhush.union.vo.UnionLinkConfigVO;
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
 * @ClassName UnionLinkConfigController
 * @Description 转链配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-转链配置", description = "转链配置的增删改查")
@RequestMapping("/admin/union/link-config")
public class UnionLinkConfigController {

    private final UnionLinkConfigService linkConfigService;

    public UnionLinkConfigController(UnionLinkConfigService linkConfigService) {
        this.linkConfigService = linkConfigService;
    }

    /**
     * 新增转链配置
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:link-config:create")
    @OperateLog(module = "转链配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增转链配置")
    public Result<Long> create(@RequestBody @Valid UnionLinkConfigParam param) {
        return Result.success(linkConfigService.createLinkConfig(param));
    }

    /**
     * 修改转链配置
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:link-config:update")
    @OperateLog(module = "转链配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改转链配置")
    public Result<Void> update(@RequestBody @Valid UnionLinkConfigParam param) {
        linkConfigService.updateLinkConfig(param);
        return Result.success();
    }

    /**
     * 删除转链配置
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:link-config:delete")
    @OperateLog(module = "转链配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除转链配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        linkConfigService.deleteLinkConfig(id);
        return Result.success();
    }

    /**
     * 转链配置详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:link-config:query")
    @Operation(summary = "转链配置详情")
    public Result<UnionLinkConfigVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(linkConfigService.getLinkConfig(id));
    }

    /**
     * 转链配置分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:link-config:query")
    @Operation(summary = "转链配置分页")
    public Result<PageResult<UnionLinkConfigVO>> page(@Valid UnionLinkConfigQuery query) {
        return Result.success(linkConfigService.getLinkConfigPage(query));
    }

}
