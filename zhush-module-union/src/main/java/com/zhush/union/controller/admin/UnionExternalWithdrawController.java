package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalWithdrawParam;
import com.zhush.union.query.UnionExternalWithdrawQuery;
import com.zhush.union.service.UnionExternalWithdrawService;
import com.zhush.union.vo.UnionExternalWithdrawVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalWithdrawController
 * @Description 外部推广者提现申请表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者提现申请表")
@RequestMapping("/admin/union/external-withdraw")
public class UnionExternalWithdrawController {

    private final UnionExternalWithdrawService externalWithdrawService;

    public UnionExternalWithdrawController(UnionExternalWithdrawService externalWithdrawService) {
        this.externalWithdrawService = externalWithdrawService;
    }

}
