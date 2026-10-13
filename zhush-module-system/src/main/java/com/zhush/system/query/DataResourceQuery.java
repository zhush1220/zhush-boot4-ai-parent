package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据资源分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "数据资源分页查询请求")
public class DataResourceQuery extends BaseQuery {

    @Schema(description = "资源名称（模糊匹配）")
    private String name;

    @Schema(description = "资源编码（模糊匹配）")
    private String code;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
