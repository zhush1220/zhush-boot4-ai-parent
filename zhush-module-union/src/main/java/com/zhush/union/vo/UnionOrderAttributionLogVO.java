package com.zhush.union.vo;

import com.zhush.union.entity.UnionOrderAttributionLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionOrderAttributionLogVO
 * @Description 订单归因日志表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionOrderAttributionLog.class)
@Schema(description = "订单归因日志表详情/列表返回")
public class UnionOrderAttributionLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "platform")
    private Integer platform;

    @Schema(description = "outer_order_id")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
