package com.zhush.system.controller.admin.vo.menu;

import com.zhush.system.entity.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单新增请求
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysMenu.class)
public class AdminMenuSaveReqVO {

    /** 父菜单ID（根为0，不传默认0） */
    private Long parentId;

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 类型：1-目录 2-菜单 3-按钮 */
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    /** 权限标识（如 system:user:add） */
    private String permission;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 菜单图标 */
    private String icon;

    /** 显示顺序 */
    private Integer sort;

    /** 是否可见：0-隐藏 1-显示（不传默认显示） */
    private Integer visible;

    /** 状态：0-禁用 1-启用（不传默认启用） */
    private Integer status;
}
