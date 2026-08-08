package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberAccountParam;
import com.zhush.union.query.UnionMemberAccountQuery;
import com.zhush.union.service.UnionMemberAccountService;
import com.zhush.union.vo.UnionMemberAccountVO;
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
 * @ClassName UnionMemberAccountController
 * @Description 推广者账户表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广者账户", description = "推广者账户的增删改查")
@RequestMapping("/admin/union/member-account")
public class UnionMemberAccountController {

    private final UnionMemberAccountService memberAccountService;

    public UnionMemberAccountController(UnionMemberAccountService memberAccountService) {
        this.memberAccountService = memberAccountService;
    }

    /**
     * 新增推广者账户
     */
    @PostMapping
    @OperateLog(module = "推广者账户", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广者账户")
    public Result<Long> create(@RequestBody @Valid UnionMemberAccountParam param) {
        return Result.success(memberAccountService.createMemberAccount(param));
    }

    /**
     * 修改推广者账户
     */
    @PutMapping
    @OperateLog(module = "推广者账户", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广者账户")
    public Result<Void> update(@RequestBody @Valid UnionMemberAccountParam param) {
        memberAccountService.updateMemberAccount(param);
        return Result.success();
    }

    /**
     * 删除推广者账户
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "推广者账户", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广者账户")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        memberAccountService.deleteMemberAccount(id);
        return Result.success();
    }

    /**
     * 推广者账户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "推广者账户详情")
    public Result<UnionMemberAccountVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(memberAccountService.getMemberAccount(id));
    }

    /**
     * 推广者账户分页
     */
    @GetMapping("/page")
    @Operation(summary = "推广者账户分页")
    public Result<PageResult<UnionMemberAccountVO>> page(@Valid UnionMemberAccountQuery query) {
        return Result.success(memberAccountService.getMemberAccountPage(query));
    }

}
