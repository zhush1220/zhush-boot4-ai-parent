package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawApplyParam;
import com.zhush.union.query.UnionWithdrawApplyQuery;
import com.zhush.union.service.UnionWithdrawApplyService;
import com.zhush.union.vo.UnionWithdrawApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionWithdrawApplyController
 * @Description 提现申请表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-提现申请表")
@RequestMapping("/admin/union/withdraw-apply")
public class UnionWithdrawApplyController {

    private final UnionWithdrawApplyService withdrawApplyService;

    public UnionWithdrawApplyController(UnionWithdrawApplyService withdrawApplyService) {
        this.withdrawApplyService = withdrawApplyService;
    }

}
