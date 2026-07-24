package com.zhush.member.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MemberUserQuery extends BaseQuery {

    /** 手机号（模糊匹配） */
    private String mobile;

    /** 昵称（模糊匹配） */
    private String nickname;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码 */
    private String registerType;
}
