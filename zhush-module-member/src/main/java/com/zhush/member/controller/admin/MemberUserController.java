package com.zhush.member.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.member.query.MemberUserQuery;
import com.zhush.member.service.MemberUserService;
import com.zhush.member.vo.MemberUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员管理接口（管理端）
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-会员管理", description = "会员用户的分页与详情查询")
@RequestMapping("/admin/member/user")
public class MemberUserController {

    private static final String TYPE = "admin";

    private final MemberUserService memberUserService;

    public MemberUserController(MemberUserService memberUserService) {
        this.memberUserService = memberUserService;
    }

    /**
     * 会员分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = TYPE, value = "member:user:query")
    @Operation(summary = "会员分页")
    public Result<PageResult<MemberUserVO>> page(@Valid MemberUserQuery query) {
        return Result.success(memberUserService.getMemberPage(query));
    }

    /**
     * 会员详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "member:user:query")
    @Operation(summary = "会员详情")
    public Result<MemberUserVO> get(@Parameter(description = "会员ID") @PathVariable Long id) {
        return Result.success(memberUserService.getMember(id));
    }

}
