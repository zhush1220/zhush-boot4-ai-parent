package com.zhush.system.vo;

import com.zhush.system.entity.SysSqlAuditLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName SqlAuditLogVO
 * @Description SQL 审计日志详情 / 列表返回
 * @Author zhush
 **/
@Data
@AutoMapper(target = SysSqlAuditLog.class)
@Schema(description = "SQL 审计日志详情 / 列表返回")
public class SqlAuditLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "平台：admin-管理端 member-会员端 system-系统任务（无请求上下文）")
    private String platform;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "请求URL")
    private String url;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "操作人")
    private String user;

    @Schema(description = "用户IP")
    private String userIp;

    @Schema(description = "主机IP")
    private String hostIp;

    @Schema(description = "数据源名称")
    private String dsName;

    @Schema(description = "Mapper 语句全限定ID")
    private String stmtId;

    @Schema(description = "原始SQL")
    private String sql;

    @Schema(description = "SQL参数（JSON）")
    private String sqlParams;

    @Schema(description = "完整SQL（含内联参数）")
    private String fullSql;

    @Schema(description = "查询返回条数")
    private Integer queryCount;

    @Schema(description = "查询发生时间")
    private LocalDateTime queryTime;

    @Schema(description = "执行耗时（毫秒）")
    private Long elapsedTime;

    @Schema(description = "扩展元信息（JSON）")
    private String metas;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
