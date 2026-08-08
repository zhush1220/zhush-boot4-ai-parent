package com.zhush.union.param;

import com.zhush.union.entity.UnionOrderAttributionLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionOrderAttributionLogParam
 * @Description 订单归因日志表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionOrderAttributionLog.class)
@Schema(description = "订单归因日志表新增/修改请求参数")
public class UnionOrderAttributionLogParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "platform")
    @NotNull(message = "platform不能为空")
    private Integer platform;

    @Schema(description = "outer_order_id")
    @NotBlank(message = "outer_order_id不能为空")
    private String outerOrderId;

    @Schema(description = "归因到的推广者ID")
    private Long attributedMemberId;

    @Schema(description = "归因方式：cookie/链接/设备ID")
    private String attributionMethod;

    @Schema(description = "置信度分数")
    private BigDecimal confidenceScore;

    @Schema(description = "0-待处理 1-成功 2-失败")
    private Integer processStatus;

    @Schema(description = "失败原因")
    private String errorMsg;

}
