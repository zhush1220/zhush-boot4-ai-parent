package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalTransactionParam;
import com.zhush.union.query.UnionExternalTransactionQuery;
import com.zhush.union.service.UnionExternalTransactionService;
import com.zhush.union.vo.UnionExternalTransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalTransactionController
 * @Description 外部推广者资金流水表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者资金流水表")
@RequestMapping("/admin/union/external-transaction")
public class UnionExternalTransactionController {

    private final UnionExternalTransactionService externalTransactionService;

    public UnionExternalTransactionController(UnionExternalTransactionService externalTransactionService) {
        this.externalTransactionService = externalTransactionService;
    }

}
