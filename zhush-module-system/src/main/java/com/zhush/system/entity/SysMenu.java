package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单 / 权限
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 父菜单ID（根为0） */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 类型：1-目录 2-菜单 3-按钮 */
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

    /** 是否可见：0-隐藏 1-显示 */
    private Integer visible;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
