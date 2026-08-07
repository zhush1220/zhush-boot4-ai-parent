package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalAccount;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalAccountParam
 * @Description 外部推广者账户表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionExternalAccount.class)
@Schema(description = "外部推广者账户表新增/修改请求参数")
public class UnionExternalAccountParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    private Long masterMemberId;

    @Schema(description = "累计佣金总额")
    private BigDecimal totalCommission;

    @Schema(description = "可提现余额")
    private BigDecimal availableBalance;

    @Schema(description = "冻结余额（提现中）")
    private BigDecimal frozenBalance;

    @Schema(description = "累计已提现")
    private BigDecimal totalWithdrawn;

    @Schema(description = "待结算佣金（未到结算周期）")
    private BigDecimal pendingSettle;

    @Schema(description = "乐观锁版本号")
    private Integer version;

}
