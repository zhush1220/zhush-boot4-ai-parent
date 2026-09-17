package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.union.service.UnionWithdrawPayoutService;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-提现出款控制器
 *
 * <p>出款编排入口：发起打款、撤销出款、主动查单。真实资金动作全部委托给
 * {@link UnionWithdrawPayoutService}，本控制器只做权限与操作日志。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-提现出款", description = "提现打款、撤单与查单")
@RequestMapping("/admin/union/withdraw-payout")
public class UnionWithdrawPayoutController {

    private final UnionWithdrawPayoutService payoutService;

    public UnionWithdrawPayoutController(UnionWithdrawPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    /**
     * 发起打款（审核通过/打款失败可重试）
     */
    @PostMapping("/{id}/payout")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:payout")
    @OperateLog(module = "提现出款", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "发起打款")
    public Result<Void> payout(@Parameter(description = "提现申请ID") @PathVariable Long id) {
        Long operatorId = StpAdminUtil.getLoginIdDefaultNull();
        payoutService.payout(id, operatorId, operatorId == null ? null : String.valueOf(operatorId));
        return Result.success();
    }

    /**
     * 撤销出款（仅渠道明确返回「已撤销」才置取消）
     */
    @PostMapping("/{id}/cancel")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:cancel")
    @OperateLog(module = "提现出款", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "撤销出款")
    public Result<Void> cancel(@Parameter(description = "提现申请ID") @PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("cancelReason");
        payoutService.cancel(id, reason);
        return Result.success();
    }

    /**
     * 主动查单收敛（V2 无回调时唯一手段；V3 亦作回调兜底）
     */
    @PostMapping("/{id}/reconcile")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:query")
    @Operation(summary = "主动查单收敛")
    public Result<Boolean> reconcile(@Parameter(description = "提现申请ID") @PathVariable Long id) {
        return Result.success(payoutService.reconcile(id));
    }

}
