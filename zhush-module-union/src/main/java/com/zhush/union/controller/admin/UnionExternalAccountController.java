package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalAccountParam;
import com.zhush.union.query.UnionExternalAccountQuery;
import com.zhush.union.service.UnionExternalAccountService;
import com.zhush.union.vo.UnionExternalAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalAccountController
 * @Description 外部推广者账户表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者账户表")
@RequestMapping("/admin/union/external-account")
public class UnionExternalAccountController {

    private final UnionExternalAccountService externalAccountService;

    public UnionExternalAccountController(UnionExternalAccountService externalAccountService) {
        this.externalAccountService = externalAccountService;
    }

}
