package com.zhush.member.vo;

import com.zhush.member.entity.MemberUser;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = MemberUser.class)
@Schema(description = "会员详情 / 列表返回")
public class MemberUserVO {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "注册IP")
    private String registerIp;

    @Schema(description = "注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码")
    private String registerType;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
