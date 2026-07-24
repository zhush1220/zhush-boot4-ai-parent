package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.Result;
import com.zhush.system.param.AdminMenuParam;
import com.zhush.system.query.AdminMenuQuery;
import com.zhush.system.service.AdminMenuService;
import com.zhush.system.vo.AdminMenuVO;
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
    public Result<Long> create(@RequestBody @Valid AdminMenuParam param) {
        return Result.success(menuService.createMenu(param));
    }

    /**
     * 修改菜单
     */
    @PutMapping
    @SaCheckPermission(type = TYPE, value = "system:menu:update")
    public Result<Void> update(@RequestBody @Valid AdminMenuParam param) {
        menuService.updateMenu(param);
        return Result.success();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

    /**
     * 菜单详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    public Result<AdminMenuVO> get(@PathVariable Long id) {
        return Result.success(menuService.getMenu(id));
    }

    /**
     * 菜单列表（扁平）
     */
    @GetMapping("/list")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    public Result<List<AdminMenuVO>> list(AdminMenuQuery query) {
        return Result.success(menuService.listMenus(query));
    }

    /**
     * 菜单树
     */
    @GetMapping("/tree")
    @SaCheckPermission(type = TYPE, value = "system:menu:query")
    public Result<List<AdminMenuVO>> tree(AdminMenuQuery query) {
        return Result.success(menuService.treeMenus(query));
    }

}
