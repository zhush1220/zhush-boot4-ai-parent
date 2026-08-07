package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionAccountTransactionQuery
 * @Description 账户资金流水表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "账户资金流水表分页查询请求")
public class UnionAccountTransactionQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "流水号（唯一）")
    private String transactionNo;

    @Schema(description = "类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚 5-税费扣除")
    private Integer transactionType;

    @Schema(description = "来源ID（如订单ID、提现申请ID）")
    private String sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "1-成功 2-失败 3-处理中")
    private Integer status;

}
