package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionWithdrawConfigQuery
 * @Description 提现配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "提现配置表分页查询请求")
public class UnionWithdrawConfigQuery extends BaseQuery {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置类型：1-数字 2-字符串 3-百分比 4-JSON")
    private Integer configType;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

}
