package com.zhush.union.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.update.UpdateChain;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.PayGateway;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.Payee;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.union.entity.UnionWithdrawApply;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.enums.UnionWithdrawStatus;
import com.zhush.union.mapper.UnionWithdrawApplyMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 提现出款编排
 *
 * <p>贯通「提现申请单 → 统一支付底座」，严格执行两阶段出款：
 * <ol>
 *     <li><b>受理</b>（单语句提交）：CAS 置「打款处理中」，确保调渠道前状态已落库；</li>
 *     <li><b>调渠道</b>（<b>事务外</b>，{@link Propagation#NOT_SUPPORTED} 挂起外层事务）：幂等键复用申请单号；</li>
 *     <li><b>记结果</b>：按渠道返回的 {@code accepted}/{@code finalState} 收敛状态；</li>
 *     <li><b>终态收敛</b>：回调（{@code UnionPayNotifyHandler}）或定时查单（{@code UnionWithdrawReconcileTask}）。</li>
 * </ol>
 *
 * <p><b>资金红线</b>：渠道「已受理」不等于「已到账」，未收敛到终态前一律保持「打款处理中」；
 * 渠道未实现/未启用时门面直接抛异常，绝不静默判成功；撤销仅在渠道明确返回
 * {@link PayStatus#CANCELLED} 时才置取消。
 *
 * @author zhush
 */
@Slf4j
@Service
public class UnionWithdrawPayoutService {

    /**
     * 渠道「本地校验」类错误码：命中说明请求未真正下发，可安全置打款失败
     */
    private static final Set<Integer> DEFINITE_LOCAL_FAILURE_CODES = Set.of(
            PayErrorCode.PAY_CHANNEL_UNSUPPORTED.getCode(),
            PayErrorCode.PAY_AMOUNT_INVALID.getCode(),
            PayErrorCode.PAYEE_INVALID.getCode());

    /** 失败原因落库最大长度 */
    private static final int FAIL_REASON_MAX_LENGTH = 500;

    private final UnionWithdrawApplyMapper withdrawApplyMapper;

    private final PayGateway payGateway;

    private final UnionWithdrawLimitService limitService;

    public UnionWithdrawPayoutService(UnionWithdrawApplyMapper withdrawApplyMapper, PayGateway payGateway,
                                      UnionWithdrawLimitService limitService) {
        this.withdrawApplyMapper = withdrawApplyMapper;
        this.payGateway = payGateway;
        this.limitService = limitService;
    }

    /**
     * 发起打款（审核通过/打款失败可重试）
     *
     * @param id           提现申请ID
     * @param operatorId   打款操作人ID
     * @param operatorName 打款操作人姓名
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void payout(Long id, Long operatorId, String operatorName) {
        UnionWithdrawApply apply = withdrawApplyMapper.selectOneById(id);
        assertNotNull(apply, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, id);
        assertTrue(UnionWithdrawStatus.canPayout(apply.getStatus()),
                UnionErrorCode.WITHDRAW_STATUS_INVALID, apply.getStatus());

        PayChannelType channelType = PayChannelType.of(apply.getPayChannel())
                .orElseThrow(() -> exception(UnionErrorCode.WITHDRAW_PAY_CHANNEL_INVALID, apply.getPayChannel()));
        Integer amount = toFen(resolvePayAmount(apply));
        assertNotNull(amount, UnionErrorCode.WITHDRAW_PAY_AMOUNT_INVALID, apply.getApplyNo());
        assertTrue(amount > 0, UnionErrorCode.WITHDRAW_PAY_AMOUNT_INVALID, apply.getApplyNo());
        // 业务侧单日累计限额（支付模块无 DB，累计口径在业务库统计）
        limitService.validateDailyLimit(apply.getMemberId(), amount, apply.getApplyNo());
        Payee payee = buildPayee(apply, channelType);

        // ① 受理：CAS 置「打款处理中」（单语句提交，调渠道前状态已持久化）
        boolean marked = markPaying(apply, operatorId, operatorName);
        assertTrue(marked, UnionErrorCode.WITHDRAW_CONCURRENT_MODIFIED, apply.getApplyNo());

        // ② 调渠道（事务外；bizNo = 申请单号，重试复用同一幂等键）
        PayoutCommand command = PayoutCommand.builder()
                .bizNo(apply.getApplyNo())
                .amount(amount)
                .channelType(channelType)
                .payee(payee)
                .remark(resolveRemark(apply))
                .build();
        PayoutResult result;
        try {
            result = payGateway.payout(command);
        } catch (RuntimeException e) {
            log.error("[UnionWithdraw] 调渠道出款异常 applyNo={}，channel={}", apply.getApplyNo(), channelType, e);
            handleChannelException(id, e);
            throw e;
        }

        // ③ 记结果（CAS：仅当仍为「打款处理中」时收敛，避免覆盖并发回调）
        recordResult(id, apply.getApplyNo(), result);
    }

    /**
     * 主动查单收敛终态（V2 无回调时唯一手段；V3 亦作回调兜底）
     *
     * @return 是否收敛到终态
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean reconcile(Long id) {
        UnionWithdrawApply apply = withdrawApplyMapper.selectOneById(id);
        assertNotNull(apply, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, id);
        if (!UnionWithdrawStatus.PAYING.is(apply.getStatus())) {
            // 非「打款处理中」无需收敛
            return false;
        }
        PayChannelType channelType = PayChannelType.of(apply.getPayChannel())
                .orElseThrow(() -> exception(UnionErrorCode.WITHDRAW_PAY_CHANNEL_INVALID, apply.getPayChannel()));

        PayoutResult result = payGateway.queryPayout(channelType, apply.getApplyNo());
        if (!result.isAccepted()) {
            // 查询失败（网络等不确定）：保持处理中，等待下次查单
            log.warn("[UnionWithdraw] 查单失败 applyNo={}，原因={}", apply.getApplyNo(), result.getFailReason());
            return false;
        }
        if (!result.getFinalState().isFinal()) {
            // 仍非终态：仅回填渠道单号
            recordChannelOrderNo(id, result.getChannelOrderNo());
            return false;
        }
        return convergeFinalState(id, apply.getApplyNo(), result);
    }

    /**
     * 撤销出款（<b>红线</b>：仅渠道明确返回「已撤销」终态才置取消，撤销中不算）
     *
     * @param id           提现申请ID
     * @param cancelReason 取消原因
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void cancel(Long id, String cancelReason) {
        UnionWithdrawApply apply = withdrawApplyMapper.selectOneById(id);
        assertNotNull(apply, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, id);
        assertTrue(UnionWithdrawStatus.PAYING.is(apply.getStatus()),
                UnionErrorCode.WITHDRAW_STATUS_INVALID, apply.getStatus());
        PayChannelType channelType = PayChannelType.of(apply.getPayChannel())
                .orElseThrow(() -> exception(UnionErrorCode.WITHDRAW_PAY_CHANNEL_INVALID, apply.getPayChannel()));
        assertTrue(payGateway.supportCancel(channelType),
                UnionErrorCode.WITHDRAW_NOT_SUPPORT_CANCEL, channelType);

        PayoutResult result = payGateway.cancelPayout(channelType, apply.getApplyNo());
        if (result.isAccepted() && result.getFinalState() == PayStatus.CANCELLED) {
            transitionToCancelled(id, StrUtil.blankToDefault(cancelReason, "渠道撤销成功"), LocalDateTime.now());
        }
    }

    /**
     * 按渠道回调/查单结果收敛提现单终态（幂等：非「打款处理中」直接忽略）
     *
     * @param applyNo        申请单号（渠道幂等键）
     * @param state          统一状态
     * @param channelOrderNo 渠道订单号
     * @param successTime    渠道完成时间（可空，空则取当前时间）
     * @param failReason     失败原因
     * @return 是否发生状态收敛
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean converge(String applyNo, PayStatus state, String channelOrderNo,
                            LocalDateTime successTime, String failReason) {
        if (state == null) {
            return false;
        }
        UnionWithdrawApply apply = withdrawApplyMapper.selectByApplyNo(applyNo);
        if (apply == null) {
            log.warn("[UnionWithdraw] 回调/查单对应的提现单不存在，applyNo={}", applyNo);
            return false;
        }
        if (!UnionWithdrawStatus.PAYING.is(apply.getStatus())) {
            // 幂等：已收敛到终态，忽略重复回调
            log.info("[UnionWithdraw] 提现单已是终态，忽略重复通知 applyNo={}，status={}", applyNo, apply.getStatus());
            return false;
        }
        Long id = apply.getId();
        LocalDateTime eventTime = successTime == null ? LocalDateTime.now() : successTime;
        return switch (state) {
            case SUCCESS -> transitionToPaid(id, channelOrderNo, eventTime);
            case CANCELLED -> transitionToCancelled(id, "渠道撤销成功", eventTime);
            case FAIL -> transitionToFailure(id, failReason);
            default -> recordChannelOrderNo(id, channelOrderNo);
        };
    }

    // ================ 渠道调用结果处理 ================

    private void handleChannelException(Long id, RuntimeException e) {
        String reason = resolveErrorReason(e);
        if (e instanceof ServiceException serviceException
                && DEFINITE_LOCAL_FAILURE_CODES.contains(serviceException.getCode())) {
            // 本地校验/渠道未实现：请求未下发，可置打款失败
            transitionToFailure(id, reason);
        } else {
            // 网络等不确定异常：渠道可能已受理，保持「打款处理中」，交由查单/回调收敛
            recordFailReason(id, reason);
        }
    }

    private void recordResult(Long id, String applyNo, PayoutResult result) {
        log.info("[UnionWithdraw] 出款渠道返回 applyNo={}，accepted={}，state={}，channelOrderNo={}",
                applyNo, result.isAccepted(), result.getFinalState(), result.getChannelOrderNo());
        PayStatus state = result.getFinalState();
        if (!result.isAccepted() || state == PayStatus.FAIL) {
            transitionToFailure(id, result.getFailReason());
            return;
        }
        if (state == PayStatus.SUCCESS) {
            // 渠道同步返回终态成功（罕见），直接置已打款
            transitionToPaid(id, result.getChannelOrderNo(), LocalDateTime.now());
            return;
        }
        if (state == PayStatus.CANCELLED) {
            transitionToCancelled(id, "渠道撤销成功", LocalDateTime.now());
            return;
        }
        // 其余（ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/TRANSFERING/CANCELING）：保持处理中，等待收敛
        recordChannelOrderNo(id, result.getChannelOrderNo());
    }

    private boolean convergeFinalState(Long id, String applyNo, PayoutResult result) {
        log.info("[UnionWithdraw] 查单收敛 applyNo={}，state={}", applyNo, result.getFinalState());
        return switch (result.getFinalState()) {
            case SUCCESS -> transitionToPaid(id, result.getChannelOrderNo(), LocalDateTime.now());
            case CANCELLED -> transitionToCancelled(id, "渠道撤销成功", LocalDateTime.now());
            case FAIL -> transitionToFailure(id, result.getFailReason());
            default -> false;
        };
    }

    // ================ CAS 状态迁移（条件：仍为「打款处理中」） ================

    private boolean markPaying(UnionWithdrawApply apply, Long operatorId, String operatorName) {
        int nextRetry = (apply.getRetryCount() == null ? 0 : apply.getRetryCount()) + 1;
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getStatus, UnionWithdrawStatus.PAYING.getCode())
                .set(UnionWithdrawApply::getPayOperatorId, operatorId)
                .set(UnionWithdrawApply::getPayOperatorName, operatorName)
                .set(UnionWithdrawApply::getRetryCount, nextRetry)
                .set(UnionWithdrawApply::getPayFailReason, "")
                .where(UnionWithdrawApply::getId).eq(apply.getId())
                .and(UnionWithdrawApply::getStatus).eq(apply.getStatus())
                .update();
    }

    private boolean transitionToPaid(Long id, String channelOrderNo, LocalDateTime payTime) {
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getStatus, UnionWithdrawStatus.PAID.getCode())
                .set(UnionWithdrawApply::getPayTime, payTime)
                .set(UnionWithdrawApply::getPayChannelOrderNo, channelOrderNo)
                .set(UnionWithdrawApply::getPayFailReason, "")
                .where(UnionWithdrawApply::getId).eq(id)
                .and(UnionWithdrawApply::getStatus).eq(UnionWithdrawStatus.PAYING.getCode())
                .update();
    }

    private boolean transitionToFailure(Long id, String failReason) {
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getStatus, UnionWithdrawStatus.PAY_FAILED.getCode())
                .set(UnionWithdrawApply::getPayFailReason, truncate(failReason))
                .where(UnionWithdrawApply::getId).eq(id)
                .and(UnionWithdrawApply::getStatus).eq(UnionWithdrawStatus.PAYING.getCode())
                .update();
    }

    private boolean transitionToCancelled(Long id, String cancelReason, LocalDateTime cancelTime) {
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getStatus, UnionWithdrawStatus.CANCELLED.getCode())
                .set(UnionWithdrawApply::getCancelTime, cancelTime)
                .set(UnionWithdrawApply::getCancelReason, truncate(cancelReason))
                .where(UnionWithdrawApply::getId).eq(id)
                .and(UnionWithdrawApply::getStatus).eq(UnionWithdrawStatus.PAYING.getCode())
                .update();
    }

    private boolean recordChannelOrderNo(Long id, String channelOrderNo) {
        if (StrUtil.isBlank(channelOrderNo)) {
            return false;
        }
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getPayChannelOrderNo, channelOrderNo)
                .where(UnionWithdrawApply::getId).eq(id)
                .and(UnionWithdrawApply::getStatus).eq(UnionWithdrawStatus.PAYING.getCode())
                .update();
    }

    private boolean recordFailReason(Long id, String failReason) {
        return UpdateChain.create(withdrawApplyMapper)
                .set(UnionWithdrawApply::getPayFailReason, truncate(failReason))
                .where(UnionWithdrawApply::getId).eq(id)
                .and(UnionWithdrawApply::getStatus).eq(UnionWithdrawStatus.PAYING.getCode())
                .update();
    }

    // ================ 组装与转换 ================

    private BigDecimal resolvePayAmount(UnionWithdrawApply apply) {
        return apply.getActualAmount() != null ? apply.getActualAmount() : apply.getApplyAmount();
    }

    /**
     * 元 → 分（金额非法时抛出业务异常，避免静默截断）
     */
    private Integer toFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        try {
            return yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException e) {
            throw exception(UnionErrorCode.WITHDRAW_PAY_AMOUNT_INVALID, yuan);
        }
    }

    private Payee buildPayee(UnionWithdrawApply apply, PayChannelType channelType) {
        if (channelType == PayChannelType.WECHAT_V3) {
            assertTrue(StrUtil.isNotBlank(apply.getPayOpenid()),
                    UnionErrorCode.WITHDRAW_PAY_OPENID_MISSING, apply.getApplyNo());
            return Payee.builder().openid(apply.getPayOpenid()).build();
        }
        // 银行卡出款（V2 企业付款到银行卡）：使用密文列解密后的完整卡号/姓名
        String accountNo = apply.getPayAccountCipher();
        String accountName = apply.getPayAccountNameCipher();
        assertTrue(StrUtil.isNotBlank(accountNo) && StrUtil.isNotBlank(accountName),
                UnionErrorCode.WITHDRAW_PAY_ACCOUNT_MISSING, apply.getApplyNo());
        return Payee.builder()
                .accountNo(accountNo)
                .accountName(accountName)
                .bankName(apply.getPayBankName())
                .bankCode(apply.getPayBankCode())
                .bankBranch(apply.getPayBankBranch())
                .build();
    }

    private String resolveRemark(UnionWithdrawApply apply) {
        return StrUtil.isBlank(apply.getRemark()) ? "佣金提现" : apply.getRemark();
    }

    private String resolveErrorReason(RuntimeException e) {
        if (e instanceof ServiceException serviceException) {
            return serviceException.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private String truncate(String text) {
        return StrUtil.subPre(text, FAIL_REASON_MAX_LENGTH);
    }

}
