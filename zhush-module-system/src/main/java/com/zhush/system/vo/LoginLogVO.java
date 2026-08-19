package com.zhush.system.vo;

import com.zhush.system.entity.SysLoginLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysLoginLog.class)
@Schema(description = "登录日志详情 / 列表返回")
public class LoginLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "登录人ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "登录人类型：admin-管理端 member-会员端")
    private String userType;

    @Schema(description = "登录IP")
    private String loginIp;

    @Schema(description = "登录IP归属地")
    private String loginRegion;

    @Schema(description = "浏览器UA")
    private String userAgent;

    @Schema(description = "登录设备")
    private String device;

    @Schema(description = "登录方式：password-账号密码 social-第三方")
    private String loginType;

    @Schema(description = "结果码：0-成功，其余为错误码")
    private Integer resultCode;

    @Schema(description = "结果提示信息")
    private String resultMsg;

    @Schema(description = "登录时间")
    private LocalDateTime createTime;

}
