package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色数据权限配置聚合返回（角色默认范围 + 按业务对象的行级覆盖）
 *
 * @author zhush
 */
@Data
@Schema(description = "角色数据权限配置聚合返回")
public class RoleDataPermissionVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色默认数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义")
    private Integer defaultScope;

    @Schema(description = "角色默认自定义部门ID列表（defaultScope=5 生效）")
    private List<Long> defaultDeptIds;

    @Schema(description = "按业务对象的行级覆盖规则")
    private List<RoleResourceRuleVO> rules;

}
