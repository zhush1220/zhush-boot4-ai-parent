package com.zhush.union.param;

import com.zhush.union.entity.UnionProductStatusLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductStatusLogParam
 * @Description 商品状态变更日志表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionProductStatusLog.class)
@Schema(description = "商品状态变更日志表新增/修改请求参数")
public class UnionProductStatusLogParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "变更字段：status/is_recommend")
    @NotBlank(message = "变更字段：status/is_recommend不能为空")
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

}
