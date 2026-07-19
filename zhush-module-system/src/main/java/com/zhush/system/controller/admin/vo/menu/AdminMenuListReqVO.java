package com.zhush.system.controller.admin.vo.menu;

import lombok.Data;

/**
 * 菜单列表查询请求（不分页，用于树形/列表过滤）
 *
 * @author zhush
 */
@Data
public class AdminMenuListReqVO {

    /** 菜单名称（模糊） */
    private String name;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
