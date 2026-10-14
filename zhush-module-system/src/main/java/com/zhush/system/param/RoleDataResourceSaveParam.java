package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色行级覆盖规则批量保存请求（整资源集合覆盖式保存）
 *
 * @author zhush
 */
@Data
@Schema(description = "角色行级覆盖规则批量保存请求")
public class RoleDataResourceSaveParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "按业务对象的行级覆盖规则集合（覆盖式：以本集合为准，未列出的资源回落角色默认）")
    @Valid
    private List<RoleResourceRuleItem> rules;

}
