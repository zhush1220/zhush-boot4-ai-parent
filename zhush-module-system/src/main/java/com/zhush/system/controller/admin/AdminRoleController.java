package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminRoleAssignDataScopeParam;
import com.zhush.system.param.AdminRoleAssignMenuParam;
import com.zhush.system.param.AdminRoleParam;
import com.zhush.system.param.RoleDataResourceSaveParam;
import com.zhush.system.param.RoleFieldPermissionParam;
import com.zhush.system.query.AdminRoleQuery;
import com.zhush.system.service.AdminRoleService;
import com.zhush.system.service.RoleDataPermissionService;
import com.zhush.system.vo.AdminRoleVO;
import com.zhush.system.vo.RoleDataPermissionVO;
import com.zhush.system.vo.RoleFieldPermissionVO;
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
 * 角色管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-角色管理", description = "角色的增删改查、菜单（权限）分配")
@RequestMapping("/admin/system/role")
public class AdminRoleController {

    private final AdminRoleService roleService;
    private final RoleDataPermissionService roleDataPermissionService;

    public AdminRoleController(AdminRoleService roleService,
                               RoleDataPermissionService roleDataPermissionService) {
        this.roleService = roleService;
        this.roleDataPermissionService = roleDataPermissionService;
    }

    /**
     * 新增角色
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:create")
    @Idempotent
    @Operation(summary = "新增角色")
    public Result<Long> create(@RequestBody @Valid AdminRoleParam param) {
        return Result.success(roleService.createRole(param));
    }

    /**
     * 修改角色
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:update")
    @Idempotent
    @Operation(summary = "修改角色")
    public Result<Void> update(@RequestBody @Valid AdminRoleParam param) {
        roleService.updateRole(param);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:delete")
    @Idempotent
    @Operation(summary = "删除角色")
    public Result<Void> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:delete")
    @Idempotent
    @Operation(summary = "批量删除角色")
    public Result<Void> deleteBatch(@Parameter(description = "角色ID列表") @RequestBody List<Long> ids) {
        roleService.deleteRoleBatch(ids);
        return Result.success();
    }

    /**
     * 角色详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:query")
    @Operation(summary = "角色详情")
    public Result<AdminRoleVO> get(@Parameter(description = "角色ID") @PathVariable Long id) {
        return Result.success(roleService.getRole(id));
    }

    /**
     * 角色分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:query")
    @Operation(summary = "角色分页")
    public Result<PageResult<AdminRoleVO>> page(@Valid AdminRoleQuery query) {
        return Result.success(roleService.getRolePage(query));
    }

    /**
     * 全部启用角色（下拉选择）
     */
    @GetMapping("/list-all")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:query")
    @Operation(summary = "全部启用角色（下拉选择）")
    public Result<List<AdminRoleVO>> listAll() {
        return Result.success(roleService.listAllEnabled());
    }

    /**
     * 分配菜单（权限）
     */
    @PutMapping("/assign-menu")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:assign-menu")
    @Idempotent
    @Operation(summary = "分配菜单（权限）")
    public Result<Void> assignMenu(@RequestBody @Valid AdminRoleAssignMenuParam param) {
        roleService.assignMenus(param.getRoleId(), param.getMenuIds());
        return Result.success();
    }

    /**
     * 分配数据范围
     */
    @PutMapping("/assign-data-scope")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:assign-data-scope")
    @Idempotent
    @Operation(summary = "分配数据范围")
    public Result<Void> assignDataScope(@RequestBody @Valid AdminRoleAssignDataScopeParam param) {
        roleService.assignDataScope(param);
        return Result.success();
    }

    /**
     * 角色数据权限配置（默认范围 + 按业务对象的行级覆盖）
     */
    @GetMapping("/{roleId}/data-permission")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:query")
    @Operation(summary = "角色数据权限配置")
    public Result<RoleDataPermissionVO> dataPermission(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(roleDataPermissionService.getDataPermission(roleId));
    }

    /**
     * 保存角色行级覆盖规则
     */
    @PutMapping("/data-permission/row")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:assign-data-scope")
    @Idempotent
    @Operation(summary = "保存角色行级覆盖规则")
    public Result<Void> saveRowRules(@RequestBody @Valid RoleDataResourceSaveParam param) {
        roleDataPermissionService.saveRowRules(param);
        return Result.success();
    }

    /**
     * 角色列级字段授权视图
     */
    @GetMapping("/{roleId}/data-permission/field")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:query")
    @Operation(summary = "角色列级字段授权视图")
    public Result<RoleFieldPermissionVO> fieldPermission(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(roleDataPermissionService.getFieldPermission(roleId));
    }

    /**
     * 保存角色列级字段授权
     */
    @PutMapping("/data-permission/field")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:role:assign-menu")
    @Idempotent
    @Operation(summary = "保存角色列级字段授权")
    public Result<Void> saveFieldPermission(@RequestBody @Valid RoleFieldPermissionParam param) {
        roleDataPermissionService.saveFieldPermissions(param);
        return Result.success();
    }

}
