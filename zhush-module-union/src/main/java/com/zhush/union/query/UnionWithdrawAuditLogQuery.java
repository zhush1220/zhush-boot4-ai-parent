package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionWithdrawAuditLogQuery
 * @Description 提现审核日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "提现审核日志表分页查询请求")
public class UnionWithdrawAuditLogQuery extends BaseQuery {

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

}
