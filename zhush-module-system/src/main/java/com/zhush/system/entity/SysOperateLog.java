package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_operate_log")
public class SysOperateLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 链路追踪ID（预留） */
    private String traceId;

    /** 操作人ID */
    private Long userId;

    /** 操作人类型：admin-管理端 member-会员端 */
    private String userType;

    /** 所属模块 */
    private String module;

    /** 操作名称 */
    private String name;

    /** 操作类型：0-其它 1-查询 2-新增 3-修改 4-删除 5-导出 6-导入 7-授权 */
    private Integer type;

    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 操作人IP */
    private String userIp;

    /** 浏览器UA */
    private String userAgent;

    /** 目标Java方法（类全名.方法名） */
    private String javaMethod;

    /** 方法入参（JSON） */
    private String javaMethodArgs;

    /** 操作开始时间 */
    private LocalDateTime startTime;

    /** 执行耗时（毫秒） */
    private Integer duration;

    /** 结果码：0-成功，其余为错误码 */
    private Integer resultCode;

    /** 结果提示信息（失败时为异常信息） */
    private String resultMsg;

    /** 返回结果（JSON） */
    private String resultData;
}
