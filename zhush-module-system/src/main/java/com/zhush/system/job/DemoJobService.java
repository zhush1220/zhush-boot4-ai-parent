package com.zhush.system.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时任务演示 Bean
 *
 * <p>作为 {@code sys_job.invoke_target} 的调用目标示例（{@code beanName.method()}），
 * 仅在模块联调/演示时使用，正式业务可删除或替换为实际业务 Bean。
 *
 * <p>方法参数约定：无参方法 {@code params} 留空；带参方法 {@code params} 传 JSON 对象，
 * 多形参按形参名取值（依赖 {@code -parameters} 编译参数）。
 *
 * @author zhush
 */
@Slf4j
@Component
public class DemoJobService {

    /**
     * 无参示例：演示任务（每 30 秒执行一次，便于观察调度日志）
     */
    public void demoTask() {
        log.info("[DemoJobService.demoTask] 执行定时任务演示，时间戳: {}", System.currentTimeMillis());
    }

    /**
     * 无参示例：清理过期数据（每天凌晨执行）
     */
    public void cleanExpiredData() {
        log.info("[DemoJobService.cleanExpiredData] 清理过期数据演示");
    }

    /**
     * 带参示例：按形参名从 {@code params} 取值
     *
     * @param orderNo    订单号
     * @param retryTimes 重试次数
     */
    public void processOrder(String orderNo, int retryTimes) {
        log.info("[DemoJobService.processOrder] 处理订单 {}，重试次数 {}", orderNo, retryTimes);
    }
}
