package com.zhush.system.vo;

import com.zhush.common.tree.TreeNode;
import com.zhush.system.entity.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @ClassName AdminMenuVO
 * @Description 菜单详情 / 树形节点返回
 * @Author zhush
 * @Date 2026/7/20 0020 22:14
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysMenu.class)
@Schema(description = "菜单详情 / 树形节点返回")
public class AdminMenuVO extends TreeNode<AdminMenuVO> {

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "类型：1-目录 2-菜单 3-按钮")
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

    @Schema(description = "是否可见：0-隐藏 1-显示")
    private Integer visible;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
