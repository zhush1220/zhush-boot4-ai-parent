package com.zhush.system.vo;

import com.zhush.common.tree.TreeNode;
import com.zhush.system.entity.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
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
public class AdminMenuVO extends TreeNode<AdminMenuVO> {

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 类型：1-目录 2-菜单 3-按钮
     */
    private Integer type;

    /**
     * 权限标识（如 system:user:add）
     */
    private String permission;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 前端组件路径
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 是否可见：0-隐藏 1-显示
     */
    private Integer visible;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
