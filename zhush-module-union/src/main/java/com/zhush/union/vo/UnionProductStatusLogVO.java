package com.zhush.union.vo;

import com.zhush.union.entity.UnionProductStatusLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductStatusLogVO
 * @Description 商品状态变更日志表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionProductStatusLog.class)
@Schema(description = "商品状态变更日志表详情/列表返回")
public class UnionProductStatusLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "变更字段：status/is_recommend")
    private String fieldName;

    @Schema(description = "旧值")
    private String oldValue;

    @Schema(description = "新值")
    private String newValue;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "变更原因")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
