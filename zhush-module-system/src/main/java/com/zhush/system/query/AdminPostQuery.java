package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "岗位分页查询请求")
public class AdminPostQuery extends BaseQuery {

    @Schema(description = "岗位编码（模糊匹配）")
    private String postCode;

    @Schema(description = "岗位名称（模糊匹配）")
    private String postName;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}
