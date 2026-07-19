package com.zhush.system.controller.admin.vo.role;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminRolePageReqVO extends BaseQuery {

    /** 角色名称（模糊匹配） */
    private String name;

    /** 角色编码（模糊匹配） */
    private String code;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
