package com.zhush.union.param;

import com.zhush.union.entity.UnionWithdrawWhitelist;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawWhitelistParam
 * @Description 提现白名单表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionWithdrawWhitelist.class)
@Schema(description = "提现白名单表新增/修改请求参数")
public class UnionWithdrawWhitelistParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控")
    @NotNull(message = "类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控不能为空")
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

}
