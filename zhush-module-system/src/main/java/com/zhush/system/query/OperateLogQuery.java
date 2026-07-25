package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @ClassName OperateLogQuery
 * @Description 操作日志分页查询请求
 * @Author zhush
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "操作日志分页查询请求")
public class OperateLogQuery extends BaseQuery {

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人类型：admin-管理端 member-会员端")
    private String userType;

    @Schema(description = "所属模块（模糊匹配）")
    private String module;

    @Schema(description = "操作名称（模糊匹配）")
    private String name;

    @Schema(description = "操作类型：0-其它 1-查询 2-新增 3-修改 4-删除 5-导出 6-导入 7-授权")
    private Integer type;

    @Schema(description = "请求地址（模糊匹配）")
    private String requestUrl;

    @Schema(description = "是否成功：true-仅成功 false-仅失败，不传查全部")
    private Boolean success;

    @Schema(description = "操作开始时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeBegin;

    @Schema(description = "操作开始时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeEnd;

}
