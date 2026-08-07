package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionProductStatusLogQuery
 * @Description 商品状态变更日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品状态变更日志表分页查询请求")
public class UnionProductStatusLogQuery extends BaseQuery {

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

}
