package com.zhush.web.operatelog.core;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志记录载体
 *
 * <p>由切面 {@code OperateLogAspect} 在方法执行前后采集填充，
 * 通过 {@link OperateLogHandler} 交给业务侧持久化，避免框架层依赖具体存储。
 *
 * @author zhush
 */
@Data
public class OperateLogRecord {

    /** 链路追踪ID（预留，接入 SkyWalking 等 APM 后填充） */
    private String traceId;

    /** 操作人ID（未登录为 null） */
    private Long userId;

    /** 操作人类型（admin / member，见 Sa-Token 登录类型） */
    private String userType;

    /** 所属模块（如 "用户管理"） */
    private String module;

    /** 操作名称（如 "新增管理员"） */
    private String name;

    /** 操作类型值，见 {@code OperateTypeEnum} */
    private Integer type;

    /** 请求方式（GET / POST / PUT / DELETE） */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 操作人IP */
    private String userIp;

    /** 浏览器 UA */
    private String userAgent;

    /** 目标 Java 方法（类全名.方法名） */
    private String javaMethod;

    /** 方法入参（JSON） */
    private String javaMethodArgs;

    /** 操作开始时间 */
    private LocalDateTime startTime;

    /** 执行耗时（毫秒） */
    private Integer duration;

    /** 结果码：0-成功，其余为业务/系统错误码 */
    private Integer resultCode;

    /** 结果提示信息（失败时为异常信息） */
    private String resultMsg;

    /** 返回结果（JSON，仅成功且开启记录时填充） */
    private String resultData;

}
