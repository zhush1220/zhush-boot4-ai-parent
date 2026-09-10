package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @ClassName SqlAuditLogQuery
 * @Description SQL 审计日志分页查询请求
 * @Author zhush
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "SQL 审计日志分页查询请求")
public class SqlAuditLogQuery extends BaseQuery {

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "平台：admin-管理端 member-会员端")
    private String platform;

    @Schema(description = "操作人（模糊匹配）")
    private String user;

    @Schema(description = "Mapper 语句全限定ID（模糊匹配）")
    private String stmtId;

    @Schema(description = "数据源名称")
    private String dsName;

    @Schema(description = "查询发生时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "查询发生时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}
