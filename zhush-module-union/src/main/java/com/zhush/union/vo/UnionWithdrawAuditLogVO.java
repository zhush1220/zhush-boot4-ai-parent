package com.zhush.union.vo;

import com.zhush.union.entity.UnionWithdrawAuditLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawAuditLogVO
 * @Description 提现审核日志表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionWithdrawAuditLog.class)
@Schema(description = "提现审核日志表详情/列表返回")
public class UnionWithdrawAuditLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "提现申请ID")
    private Long applyId;

    @Schema(description = "申请单号")
    private String applyNo;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理")
    private Integer operationType;

    @Schema(description = "操作前状态")
    private Integer beforeStatus;

    @Schema(description = "操作后状态")
    private Integer afterStatus;

    @Schema(description = "操作备注")
    private String remark;

    @Schema(description = "extra_json")
    private String extraJson;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
