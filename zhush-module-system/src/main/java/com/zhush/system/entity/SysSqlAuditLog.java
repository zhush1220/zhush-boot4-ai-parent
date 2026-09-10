package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SQL 审计日志
 *
 * <p>由框架层 {@code SqlAuditMessageReporter}（原生 JdbcTemplate）写入，本实体仅用于管理端读取/删除。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_sql_audit_log")
public class SysSqlAuditLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 链路追踪ID */
    private String traceId;

    /** 平台：admin-管理端 member-会员端 */
    private String platform;

    /** 所属模块 */
    private String module;

    /** 请求URL */
    private String url;

    /** 业务ID */
    private String bizId;

    /** 操作人（用户ID/用户名） */
    private String user;

    /** 用户IP */
    private String userIp;

    /** 主机IP */
    private String hostIp;

    /** 数据源名称 */
    private String dsName;

    /** Mapper 语句全限定ID（com.xxx.Mapper.method） */
    private String stmtId;

    /** 原始SQL文本 */
    private String sql;

    /** SQL参数（JSON） */
    private String sqlParams;

    /** 完整SQL（含内联参数） */
    private String fullSql;

    /** 查询返回条数 */
    private Integer queryCount;

    /** 查询发生时间 */
    private LocalDateTime queryTime;

    /** 执行耗时（毫秒） */
    private Long elapsedTime;

    /** 扩展元信息（JSON） */
    private String metas;
}
