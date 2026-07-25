package com.zhush.system.param;

import com.zhush.system.entity.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName AdminMenuParam
 * @Description 菜单新增/修改请求参数
 * @Author zhush
 * @Date 2026/7/20 0020 22:09
 **/
@Data
@AutoMapper(target = SysMenu.class)
@Schema(description = "菜单新增/修改请求参数")
public class AdminMenuParam {

    @Schema(description = "菜单ID（新增时不需要传）")
    private Long id;

    @Schema(description = "父菜单ID（根为0，不传默认0）")
    private Long parentId;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @Schema(description = "类型：1-目录 2-菜单 3-按钮", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Schema(description = "权限标识（如 system:user:add）")
    private String permission;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "是否可见：0-隐藏 1-显示（不传默认显示）")
    private Integer visible;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

}
