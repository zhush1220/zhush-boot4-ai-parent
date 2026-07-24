package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName AdminRoleQuery
 * @Description 角色分页查询请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminRoleQuery extends BaseQuery {

    /**
     * 角色名称（模糊匹配）
     */
    private String name;

    /**
     * 角色编码（模糊匹配）
     */
    private String code;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

}
