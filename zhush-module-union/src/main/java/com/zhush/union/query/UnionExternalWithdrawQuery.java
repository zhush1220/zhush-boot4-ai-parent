package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionExternalWithdrawQuery
 * @Description 外部推广者提现申请表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "外部推广者提现申请表分页查询请求")
public class UnionExternalWithdrawQuery extends BaseQuery {

    @Schema(description = "external_promoter_id")
    private Long externalPromoterId;

    @Schema(description = "master_member_id")
    private Long masterMemberId;

    @Schema(description = "申请单号")
    private String applyNo;

    @Schema(description = "收款账号")
    private String payAccount;

    @Schema(description = "收款人姓名")
    private String payAccountName;

    @Schema(description = "1-待处理 2-已打款 3-已驳回")
    private Integer status;

    @Schema(description = "transaction_id")
    private Long transactionId;

}
