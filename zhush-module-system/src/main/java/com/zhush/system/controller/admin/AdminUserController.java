package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.controller.admin.vo.user.AdminUserAssignRoleReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserPageReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserRespVO;
import com.zhush.system.controller.admin.vo.user.AdminUserResetPwdReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserSaveReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserUpdateReqVO;
import com.zhush.system.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口
 *
 * @author zhush
 */
@RestController
@RequestMapping("/admin/system/user")
public class AdminUserController {

    private static final String TYPE = "admin";

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    /**
     * 新增管理员
     */
    @PostMapping
    @SaCheckPermission(type = TYPE, value = "system:user:create")
    public Result<Long> create(@RequestBody @Valid AdminUserSaveReqVO reqVO) {
        return Result.success(userService.createUser(reqVO));
    }

    /**
     * 修改管理员
     */
    @PutMapping
    @SaCheckPermission(type = TYPE, value = "system:user:update")
    public Result<Void> update(@RequestBody @Valid AdminUserUpdateReqVO reqVO) {
        userService.updateUser(reqVO);
        return Result.success();
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:user:delete")
    public Result<Void> delete(@org.springframework.web.bind.annotation.PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 管理员详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "system:user:query")
    public Result<AdminUserRespVO> get(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return Result.success(userService.getUser(id));
    }

    /**
     * 管理员分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = TYPE, value = "system:user:query")
    public Result<PageResult<AdminUserRespVO>> page(@Valid AdminUserPageReqVO reqVO) {
        return Result.success(userService.getUserPage(reqVO));
    }

    /**
     * 分配角色
     */
    @PutMapping("/assign-role")
    @SaCheckPermission(type = TYPE, value = "system:user:assign-role")
    public Result<Void> assignRole(@RequestBody @Valid AdminUserAssignRoleReqVO reqVO) {
        userService.assignRoles(reqVO.getUserId(), reqVO.getRoleIds());
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password")
    @SaCheckPermission(type = TYPE, value = "system:user:reset-password")
    public Result<Void> resetPassword(@RequestBody @Valid AdminUserResetPwdReqVO reqVO) {
        userService.resetPassword(reqVO.getId(), reqVO.getPassword());
        return Result.success();
    }

    /**
     * 修改状态（0-禁用 1-启用）
     */
    @PutMapping("/update-status")
    @SaCheckPermission(type = TYPE, value = "system:user:update")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        AdminUserUpdateReqVO reqVO = new AdminUserUpdateReqVO();
        reqVO.setId(id);
        reqVO.setStatus(status);
        userService.updateUser(reqVO);
        return Result.success();
    }

}
