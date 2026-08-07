package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionWithdrawWhitelistQuery
 * @Description 提现白名单表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "提现白名单表分页查询请求")
public class UnionWithdrawWhitelistQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控")
    private Integer whitelistType;

    @Schema(description = "加入原因")
    private String reason;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "1-生效 0-已失效")
    private Integer status;

}
