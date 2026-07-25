package com.zhush.member.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "会员分页查询请求")
public class MemberUserQuery extends BaseQuery {

    @Schema(description = "手机号（模糊匹配）")
    private String mobile;

    @Schema(description = "昵称（模糊匹配）")
    private String nickname;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码")
    private String registerType;
}
