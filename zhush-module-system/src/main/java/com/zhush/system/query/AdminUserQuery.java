package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName AdminUserQuery
 * @Description 管理员分页查询请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminUserQuery extends BaseQuery {

    /**
     * 登录账号（模糊匹配）
     */
    private String username;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

}
