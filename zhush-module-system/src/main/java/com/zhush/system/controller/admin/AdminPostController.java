package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminPostParam;
import com.zhush.system.query.AdminPostQuery;
import com.zhush.system.service.AdminPostService;
import com.zhush.system.vo.AdminPostVO;
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
 * 岗位管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-岗位管理", description = "岗位的增删改查与分页列表")
@RequestMapping("/admin/system/post")
public class AdminPostController {

    private final AdminPostService postService;

    public AdminPostController(AdminPostService postService) {
        this.postService = postService;
    }

    /**
     * 新增岗位
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:post:create")
    @Idempotent
    @OperateLog(module = "岗位管理", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增岗位")
    public Result<Long> create(@RequestBody @Valid AdminPostParam param) {
        return Result.success(postService.createPost(param));
    }

    /**
     * 修改岗位
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:post:update")
    @Idempotent
    @OperateLog(module = "岗位管理", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改岗位")
    public Result<Void> update(@RequestBody @Valid AdminPostParam param) {
        postService.updatePost(param);
        return Result.success();
    }

    /**
     * 删除岗位
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:post:delete")
    @Idempotent
    @OperateLog(module = "岗位管理", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除岗位")
    public Result<Void> delete(@Parameter(description = "岗位ID") @PathVariable Long id) {
        postService.deletePost(id);
        return Result.success();
    }

    /**
     * 岗位详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:post:query")
    @Operation(summary = "岗位详情")
    public Result<AdminPostVO> get(@Parameter(description = "岗位ID") @PathVariable Long id) {
        return Result.success(postService.getPost(id));
    }

    /**
     * 岗位分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:post:query")
    @Operation(summary = "岗位分页")
    public Result<PageResult<AdminPostVO>> page(@Valid AdminPostQuery query) {
        return Result.success(postService.getPostPage(query));
    }
}
