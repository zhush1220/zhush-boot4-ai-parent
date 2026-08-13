package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminMenuParam;
import com.zhush.system.query.AdminMenuQuery;
import com.zhush.system.service.AdminMenuService;
import com.zhush.system.vo.AdminMenuVO;
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

import java.util.List;

/**
 * 菜单管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-菜单管理", description = "菜单（权限）的增删改查、列表与树形结构")
@RequestMapping("/admin/system/menu")
public class AdminMenuController {

    private static final String TYPE = "admin";

    private final AdminMenuService menuService;

    public AdminMenuController(AdminMenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @SaCheckPermission(type = TYPE, value = "system:menu:create")
    @Idempotent
    @Operation(summary = "新增菜单")
    public Result<Long> create(@RequestBody @Valid AdminMenuParam param) {
        return Result.success(menuService.createMenu(param));
    }

    /**
     * 修改菜单
     */
    @PutMapping
    @SaCheckPermission(type = TYPE, value = "system:menu:update")
    @Idempotent
    @Operation(summary = "修改菜单")
    public Result<Void> update(@RequestBody @Valid AdminMenuParam param) {
        menuService.updateMenu(param);
        return Result.success();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:menu:delete")
    @Idempotent
    @Operation(summary = "删除菜单")
    public Result<Void> delete(@Parameter(description = "菜单ID") @PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

    /**
     * 菜单详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    @Operation(summary = "菜单详情")
    public Result<AdminMenuVO> get(@Parameter(description = "菜单ID") @PathVariable Long id) {
        return Result.success(menuService.getMenu(id));
    }

    /**
     * 菜单列表（扁平）
     */
    @GetMapping("/list")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    @Operation(summary = "菜单列表（扁平）")
    public Result<List<AdminMenuVO>> list(AdminMenuQuery query) {
        return Result.success(menuService.listMenus(query));
    }

    /**
     * 菜单树
     */
    @GetMapping("/tree")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    @Operation(summary = "菜单树")
    public Result<List<AdminMenuVO>> tree(AdminMenuQuery query) {
        return Result.success(menuService.treeMenus(query));
    }

}
