package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionAccountTransactionParam;
import com.zhush.union.query.UnionAccountTransactionQuery;
import com.zhush.union.service.UnionAccountTransactionService;
import com.zhush.union.vo.UnionAccountTransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionAccountTransactionController
 * @Description 账户资金流水表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-账户资金流水表")
@RequestMapping("/admin/union/account-transaction")
public class UnionAccountTransactionController {

    private final UnionAccountTransactionService accountTransactionService;

    public UnionAccountTransactionController(UnionAccountTransactionService accountTransactionService) {
        this.accountTransactionService = accountTransactionService;
    }

}
