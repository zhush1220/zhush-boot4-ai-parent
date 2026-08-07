package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawRiskRecordParam;
import com.zhush.union.query.UnionWithdrawRiskRecordQuery;
import com.zhush.union.service.UnionWithdrawRiskRecordService;
import com.zhush.union.vo.UnionWithdrawRiskRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionWithdrawRiskRecordController
 * @Description 提现风控记录表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-提现风控记录表")
@RequestMapping("/admin/union/withdraw-risk-record")
public class UnionWithdrawRiskRecordController {

    private final UnionWithdrawRiskRecordService withdrawRiskRecordService;

    public UnionWithdrawRiskRecordController(UnionWithdrawRiskRecordService withdrawRiskRecordService) {
        this.withdrawRiskRecordService = withdrawRiskRecordService;
    }

}
