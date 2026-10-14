package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色单资源行级覆盖规则项（前端数组 ↔ 后端逗号串由 Service 统一转换）
 *
 * @author zhush
 */
@Data
@Schema(description = "角色单资源行级覆盖规则项")
public class RoleResourceRuleItem {

    @Schema(description = "资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "资源编码不能为空")
    private String resourceCode;

    @Schema(description = "数据范围：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义部门 6-自定义条件；null 表示清除覆盖回落角色默认")
    @Min(value = 1, message = "数据范围取值范围 1-6")
    @Max(value = 6, message = "数据范围取值范围 1-6")
    private Integer scopeType;

    @Schema(description = "自定义部门ID列表（scopeType=5 必传）")
    private List<Long> deptIds;

    @Schema(description = "自定义条件 JSON（scopeType=6 必传，字段/操作符需在资源白名单内）")
    private String conditionJson;

}
