package com.zhush.web.operatelog.core;

/**
 * 操作日志处理器
 *
 * <p>框架层切面只负责采集 {@link OperateLogRecord}，具体持久化（写库/写MQ/上报日志中心）
 * 由业务模块实现本接口并注册为 Spring Bean（如 system 模块异步写入 {@code sys_operate_log} 表）。
 * 未提供实现时，切面降级为仅打印日志。
 *
 * @author zhush
 */
@FunctionalInterface
public interface OperateLogHandler {

    /**
     * 处理一条操作日志
     *
     * <p>该方法在请求线程中被调用，实现方应尽量异步处理（如 {@code @Async} 或线程池），
     * 且内部异常不应影响业务主流程（切面已兜底捕获）。
     *
     * @param record 操作日志记录
     */
    void handle(OperateLogRecord record);

}
