package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.DataResourceUpdateParam;
import com.zhush.system.query.DataResourceQuery;
import com.zhush.system.service.DataResourceService;
import com.zhush.system.vo.DataFieldVO;
import com.zhush.system.vo.DataResourceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据资源管理接口（元数据同步、资源查询、受控字段、资源编辑）
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-数据资源管理", description = "数据资源登记、元数据同步、受控字段管理")
@RequestMapping("/admin/system/data-resource")
public class AdminDataResourceController {

    private final DataResourceService dataResourceService;

    public AdminDataResourceController(DataResourceService dataResourceService) {
        this.dataResourceService = dataResourceService;
    }

    /**
     * 元数据同步（扫描代码注解登记/刷新资源与字段）
     */
    @PostMapping("/sync")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:sync")
    @Idempotent
    @Operation(summary = "元数据同步")
    public Result<String> sync() {
        var result = dataResourceService.sync();
        return Result.success("同步完成：资源 " + result.resourceCount + " 个，字段 " + result.fieldCount + " 个");
    }

    /**
     * 资源分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:query")
    @Operation(summary = "数据资源分页")
    public Result<PageResult<DataResourceVO>> page(@Valid DataResourceQuery query) {
        return Result.success(dataResourceService.page(query));
    }

    /**
     * 全部启用资源（角色数据权限配置页 Tab 数据源）
     */
    @GetMapping("/list")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:query")
    @Operation(summary = "全部启用资源")
    public Result<List<DataResourceVO>> list() {
        return Result.success(dataResourceService.listEnabled());
    }

    /**
     * 资源详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:query")
    @Operation(summary = "数据资源详情")
    public Result<DataResourceVO> get(@Parameter(description = "资源ID") @PathVariable Long id) {
        return Result.success(dataResourceService.get(id));
    }

    /**
     * 资源受控字段列表
     */
    @GetMapping("/{id}/fields")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:query")
    @Operation(summary = "资源受控字段列表")
    public Result<List<DataFieldVO>> fields(@Parameter(description = "资源ID") @PathVariable Long id) {
        return Result.success(dataResourceService.listFields(id));
    }

    /**
     * 编辑资源
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:update")
    @Idempotent
    @Operation(summary = "编辑数据资源")
    public Result<Void> update(@RequestBody @Valid DataResourceUpdateParam param) {
        dataResourceService.update(param);
        return Result.success();
    }

    /**
     * 启停受控字段
     */
    @PutMapping("/field/{fieldId}/toggle")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:data-resource:update")
    @Idempotent
    @Operation(summary = "启停受控字段")
    public Result<Void> toggleField(@Parameter(description = "字段ID") @PathVariable Long fieldId,
                                    @Parameter(description = "状态：0-否 1-是") @RequestParam Integer status) {
        dataResourceService.toggleField(fieldId, status);
        return Result.success();
    }

}
