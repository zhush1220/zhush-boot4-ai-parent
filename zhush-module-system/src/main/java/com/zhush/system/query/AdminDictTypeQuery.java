package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字典类型分页查询请求")
public class AdminDictTypeQuery extends BaseQuery {

    @Schema(description = "字典名称（模糊匹配）")
    private String name;

    @Schema(description = "字典类型编码（模糊匹配）")
    private String type;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}
