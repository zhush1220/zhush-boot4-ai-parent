package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberAccountParam;
import com.zhush.union.query.UnionMemberAccountQuery;
import com.zhush.union.service.UnionMemberAccountService;
import com.zhush.union.vo.UnionMemberAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionMemberAccountController
 * @Description 推广者账户表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广者账户表")
@RequestMapping("/admin/union/member-account")
public class UnionMemberAccountController {

    private final UnionMemberAccountService memberAccountService;

    public UnionMemberAccountController(UnionMemberAccountService memberAccountService) {
        this.memberAccountService = memberAccountService;
    }

}
