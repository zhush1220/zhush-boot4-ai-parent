package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色在单个业务对象上的行级覆盖规则返回
 *
 * @author zhush
 */
@Data
@Schema(description = "角色单资源行级覆盖规则")
public class RoleResourceRuleVO {

    @Schema(description = "资源编码")
    private String resourceCode;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "数据范围：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义部门 6-自定义条件；null 表示未覆盖，回落角色默认")
    private Integer scopeType;

    @Schema(description = "自定义部门ID列表（scopeType=5 生效）")
    private List<Long> deptIds;

    @Schema(description = "自定义条件 JSON（scopeType=6 生效）")
    private String conditionJson;

}
