package com.zhush.system.vo;

import com.zhush.system.entity.SysOperateLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName OperateLogVO
 * @Description 操作日志详情 / 列表返回
 * @Author zhush
 **/
@Data
@AutoMapper(target = SysOperateLog.class)
@Schema(description = "操作日志详情 / 列表返回")
public class OperateLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人类型：admin-管理端 member-会员端")
    private String userType;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "操作名称")
    private String name;

    @Schema(description = "操作类型：0-其它 1-查询 2-新增 3-修改 4-删除 5-导出 6-导入 7-授权")
    private Integer type;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "操作人IP")
    private String userIp;

    @Schema(description = "浏览器UA")
    private String userAgent;

    @Schema(description = "目标Java方法")
    private String javaMethod;

    @Schema(description = "方法入参（JSON）")
    private String javaMethodArgs;

    @Schema(description = "操作开始时间")
    private LocalDateTime startTime;

    @Schema(description = "执行耗时（毫秒）")
    private Integer duration;

    @Schema(description = "结果码：0-成功，其余为错误码")
    private Integer resultCode;

    @Schema(description = "结果提示信息")
    private String resultMsg;

    @Schema(description = "返回结果（JSON）")
    private String resultData;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
