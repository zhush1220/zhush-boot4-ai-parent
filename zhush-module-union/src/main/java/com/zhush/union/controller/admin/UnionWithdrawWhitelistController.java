package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawWhitelistParam;
import com.zhush.union.query.UnionWithdrawWhitelistQuery;
import com.zhush.union.service.UnionWithdrawWhitelistService;
import com.zhush.union.vo.UnionWithdrawWhitelistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionWithdrawWhitelistController
 * @Description 提现白名单表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-提现白名单表")
@RequestMapping("/admin/union/withdraw-whitelist")
public class UnionWithdrawWhitelistController {

    private final UnionWithdrawWhitelistService withdrawWhitelistService;

    public UnionWithdrawWhitelistController(UnionWithdrawWhitelistService withdrawWhitelistService) {
        this.withdrawWhitelistService = withdrawWhitelistService;
    }

}
