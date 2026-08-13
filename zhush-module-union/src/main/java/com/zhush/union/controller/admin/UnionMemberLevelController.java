package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberLevelParam;
import com.zhush.union.query.UnionMemberLevelQuery;
import com.zhush.union.service.UnionMemberLevelService;
import com.zhush.union.vo.UnionMemberLevelVO;
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
 * @ClassName UnionMemberLevelController
 * @Description 推广者等级配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广者等级配置", description = "推广者等级配置的增删改查")
@RequestMapping("/admin/union/member-level")
public class UnionMemberLevelController {

    private final UnionMemberLevelService memberLevelService;

    public UnionMemberLevelController(UnionMemberLevelService memberLevelService) {
        this.memberLevelService = memberLevelService;
    }

    /**
     * 新增推广者等级配置
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:member-level:create")
    @OperateLog(module = "推广者等级配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广者等级配置")
    public Result<Long> create(@RequestBody @Valid UnionMemberLevelParam param) {
        return Result.success(memberLevelService.createMemberLevel(param));
    }

    /**
     * 修改推广者等级配置
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:member-level:update")
    @OperateLog(module = "推广者等级配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广者等级配置")
    public Result<Void> update(@RequestBody @Valid UnionMemberLevelParam param) {
        memberLevelService.updateMemberLevel(param);
        return Result.success();
    }

    /**
     * 删除推广者等级配置
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:member-level:delete")
    @OperateLog(module = "推广者等级配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广者等级配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        memberLevelService.deleteMemberLevel(id);
        return Result.success();
    }

    /**
     * 推广者等级配置详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:member-level:query")
    @Operation(summary = "推广者等级配置详情")
    public Result<UnionMemberLevelVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(memberLevelService.getMemberLevel(id));
    }

    /**
     * 推广者等级配置分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:member-level:query")
    @Operation(summary = "推广者等级配置分页")
    public Result<PageResult<UnionMemberLevelVO>> page(@Valid UnionMemberLevelQuery query) {
        return Result.success(memberLevelService.getMemberLevelPage(query));
    }

}
