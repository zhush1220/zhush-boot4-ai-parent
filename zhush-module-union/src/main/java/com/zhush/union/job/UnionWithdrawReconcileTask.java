package com.zhush.union.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 提现出款对账拉单任务
 *
 * <p>扫描「打款处理中」的提现单，主动向渠道查单收敛终态；是回调之外的必要兜底
 * （V2 企业付款无回调，只能靠查单）。对超 SLA 未收敛/查单异常的单据记入对账差异表并告警。
 *
 * <p>以普通 {@code @Component} Bean 暴露无参方法，由 {@code sys_job} 表驱动调用：
 * {@code invoke_target = unionWithdrawReconcileTask.reconcile()}。
 *
 * @author zhush
 */
@Slf4j
@Component
public class UnionWithdrawReconcileTask {


}

