package com.zhush.union.vo;

import com.zhush.union.entity.UnionMemberAccount;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberAccountVO
 * @Description 推广者账户表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionMemberAccount.class)
@Schema(description = "推广者账户表详情/列表返回")
public class UnionMemberAccountVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "推广者ID")
    private Long memberId;

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

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
