package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.controller.admin.vo.role.AdminRoleAssignMenuReqVO;
import com.zhush.system.controller.admin.vo.role.AdminRolePageReqVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleRespVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleSaveReqVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleUpdateReqVO;
import com.zhush.system.service.AdminRoleService;
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
@RequestMapping("/admin/system/role")
public class AdminRoleController {

    private static final String TYPE = "admin";

    private final AdminRoleService roleService;

    public AdminRoleController(AdminRoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 新增角色
     */
    @PostMapping
    @SaCheckPermission(type = TYPE, value = "system:role:create")
    public Result<Long> create(@RequestBody @Valid AdminRoleSaveReqVO reqVO) {
        return Result.success(roleService.createRole(reqVO));
    }

    /**
     * 修改角色
     */
    @PutMapping
    @SaCheckPermission(type = TYPE, value = "system:role:update")
    public Result<Void> update(@RequestBody @Valid AdminRoleUpdateReqVO reqVO) {
        roleService.updateRole(reqVO);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 角色详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:role:query")
    public Result<AdminRoleRespVO> get(@PathVariable Long id) {
        return Result.success(roleService.getRole(id));
    }

    /**
     * 角色分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = TYPE, value = "system:role:query")
    public Result<PageResult<AdminRoleRespVO>> page(@Valid AdminRolePageReqVO reqVO) {
        return Result.success(roleService.getRolePage(reqVO));
    }

    /**
     * 全部启用角色（下拉选择）
     */
    @GetMapping("/list-all")
    @SaCheckPermission(type = TYPE, value = "system:role:query")
    public Result<List<AdminRoleRespVO>> listAll() {
        return Result.success(roleService.listAllEnabled());
    }

    /**
     * 分配菜单（权限）
     */
    @PutMapping("/assign-menu")
    @SaCheckPermission(type = TYPE, value = "system:role:assign-menu")
    public Result<Void> assignMenu(@RequestBody @Valid AdminRoleAssignMenuReqVO reqVO) {
        roleService.assignMenus(reqVO.getRoleId(), reqVO.getMenuIds());
        return Result.success();
    }

}
