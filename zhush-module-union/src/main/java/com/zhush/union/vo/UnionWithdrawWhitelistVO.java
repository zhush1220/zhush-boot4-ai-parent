package com.zhush.union.vo;

import com.zhush.union.entity.UnionWithdrawWhitelist;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawWhitelistVO
 * @Description 提现白名单表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionWithdrawWhitelist.class)
@Schema(description = "提现白名单表详情/列表返回")
public class UnionWithdrawWhitelistVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控")
    private Integer whitelistType;

    @Schema(description = "额度上限（NULL表示不限制）")
    private BigDecimal limitAmount;

    @Schema(description = "加入原因")
    private String reason;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "过期时间（NULL表示永久）")
    private LocalDateTime expireTime;

    @Schema(description = "1-生效 0-已失效")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
