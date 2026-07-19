package com.zhush.system.controller.admin.vo.user;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminUserPageReqVO extends BaseQuery {

    /** 登录账号（模糊匹配） */
    private String username;

    /** 手机号 */
    private String mobile;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
