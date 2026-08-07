package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawAuditLogParam;
import com.zhush.union.query.UnionWithdrawAuditLogQuery;
import com.zhush.union.service.UnionWithdrawAuditLogService;
import com.zhush.union.vo.UnionWithdrawAuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionWithdrawAuditLogController
 * @Description 提现审核日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-提现审核日志表")
@RequestMapping("/admin/union/withdraw-audit-log")
public class UnionWithdrawAuditLogController {

    private final UnionWithdrawAuditLogService withdrawAuditLogService;

    public UnionWithdrawAuditLogController(UnionWithdrawAuditLogService withdrawAuditLogService) {
        this.withdrawAuditLogService = withdrawAuditLogService;
    }

}
