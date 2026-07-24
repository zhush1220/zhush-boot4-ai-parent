package com.zhush.member.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.member.query.MemberUserQuery;
import com.zhush.member.service.MemberUserService;
import com.zhush.member.vo.MemberUserVO;
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
    public Result<PageResult<MemberUserVO>> page(@Valid MemberUserQuery query) {
        return Result.success(memberUserService.getMemberPage(query));
    }

    /**
     * 会员详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = TYPE, value = "member:user:query")
    public Result<MemberUserVO> get(@PathVariable Long id) {
        return Result.success(memberUserService.getMember(id));
    }

}
