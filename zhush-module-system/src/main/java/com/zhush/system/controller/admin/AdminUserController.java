package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminUserAssignRoleParam;
import com.zhush.system.param.AdminUserParam;
import com.zhush.system.param.AdminUserResetPwdParam;
import com.zhush.system.query.AdminUserQuery;
import com.zhush.system.service.AdminUserService;
import com.zhush.system.vo.AdminUserVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员用户管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-用户管理", description = "管理员用户的增删改查、分配角色、重置密码等")
@RequestMapping("/admin/system/user")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    /**
     * 新增管理员
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:create")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.CREATE, recordParams = false)
    @Operation(summary = "新增管理员")
    public Result<Long> create(@RequestBody @Valid AdminUserParam param) {
        return Result.success(userService.createUser(param));
    }

    /**
     * 修改管理员
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:update")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改管理员")
    public Result<Void> update(@RequestBody @Valid AdminUserParam param) {
        userService.updateUser(param);
        return Result.success();
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:delete")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除管理员")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 批量删除管理员
     */
    @DeleteMapping("/batch")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:delete")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.DELETE)
    @Operation(summary = "批量删除管理员")
    public Result<Void> deleteBatch(@Parameter(description = "用户ID列表") @RequestBody List<Long> ids) {
        userService.deleteUserBatch(ids);
        return Result.success();
    }

    /**
     * 管理员详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:query")
    @Operation(summary = "管理员详情")
    public Result<AdminUserVO> get(@Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.success(userService.getUser(id));
    }

    /**
     * 管理员分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:query")
    @Operation(summary = "管理员分页")
    public Result<PageResult<AdminUserVO>> page(@Valid AdminUserQuery query) {
        return Result.success(userService.getUserPage(query));
    }

    /**
     * 分配角色
     */
    @PutMapping("/assign-role")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:assign-role")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.GRANT)
    @Operation(summary = "分配角色")
    public Result<Void> assignRole(@RequestBody @Valid AdminUserAssignRoleParam param) {
        userService.assignRoles(param.getUserId(), param.getRoleIds());
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:reset-password")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.GRANT, recordParams = false)
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@RequestBody @Valid AdminUserResetPwdParam param) {
        userService.resetPassword(param.getId(), param.getPassword());
        return Result.success();
    }

    /**
     * 修改状态（0-禁用 1-启用）
     */
    @PutMapping("/update-status")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:user:update")
    @Idempotent
    @OperateLog(module = "用户管理", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改状态（0-禁用 1-启用）")
    public Result<Void> updateStatus(@Parameter(description = "用户ID") @RequestParam Long id,
                                     @Parameter(description = "状态：0-禁用 1-启用") @RequestParam Integer status) {
        AdminUserParam param = new AdminUserParam();
        param.setId(id);
        param.setStatus(status);
        userService.updateUser(param);
        return Result.success();
    }

}
