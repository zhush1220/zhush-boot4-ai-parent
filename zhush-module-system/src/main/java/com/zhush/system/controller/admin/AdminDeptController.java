package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminDeptParam;
import com.zhush.system.query.AdminDeptQuery;
import com.zhush.system.service.AdminDeptService;
import com.zhush.system.vo.AdminDeptVO;
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
 * 部门管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-部门管理", description = "部门的增删改查、列表与树形结构")
@RequestMapping("/admin/system/dept")
public class AdminDeptController {

    private final AdminDeptService deptService;

    public AdminDeptController(AdminDeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 新增部门
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:create")
    @Idempotent
    @Operation(summary = "新增部门")
    public Result<Long> create(@RequestBody @Valid AdminDeptParam param) {
        return Result.success(deptService.createDept(param));
    }

    /**
     * 修改部门
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:update")
    @Idempotent
    @Operation(summary = "修改部门")
    public Result<Void> update(@RequestBody @Valid AdminDeptParam param) {
        deptService.updateDept(param);
        return Result.success();
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:delete")
    @Idempotent
    @Operation(summary = "删除部门")
    public Result<Void> delete(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }

    /**
     * 部门详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:query")
    @Operation(summary = "部门详情")
    public Result<AdminDeptVO> get(@Parameter(description = "部门ID") @PathVariable Long id) {
        return Result.success(deptService.getDept(id));
    }

    /**
     * 部门列表（扁平）
     */
    @GetMapping("/list")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:query")
    @Operation(summary = "部门列表（扁平）")
    public Result<List<AdminDeptVO>> list(AdminDeptQuery query) {
        return Result.success(deptService.listDepts(query));
    }

    /**
     * 部门树
     */
    @GetMapping("/tree")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dept:query")
    @Operation(summary = "部门树")
    public Result<List<AdminDeptVO>> tree(AdminDeptQuery query) {
        return Result.success(deptService.treeDepts(query));
    }

}
