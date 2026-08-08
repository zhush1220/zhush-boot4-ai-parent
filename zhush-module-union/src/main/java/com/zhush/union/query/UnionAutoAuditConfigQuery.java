package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionAutoAuditConfigQuery
 * @Description 自动审核配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "自动审核配置表分页查询请求")
public class UnionAutoAuditConfigQuery extends BaseQuery {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "描述")
    private String description;

}
