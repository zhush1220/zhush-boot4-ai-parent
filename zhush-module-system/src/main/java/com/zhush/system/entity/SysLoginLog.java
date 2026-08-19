package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录日志
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_login_log")
public class SysLoginLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录人ID（账号不存在等场景为 NULL） */
    private Long userId;

    /** 登录账号（登录时填写的账号） */
    private String username;

    /** 登录人类型：admin-管理端 member-会员端 */
    private String userType;

    /** 登录IP */
    private String loginIp;

    /** 登录IP归属地（如 中国 广东省 深圳市） */
    private String loginRegion;

    /** 浏览器UA */
    private String userAgent;

    /** 登录设备：PC/H5/APP 等 */
    private String device;

    /** 登录方式：password-账号密码 social-第三方 */
    private String loginType;

    /** 结果码：0-成功，其余为错误码 */
    private Integer resultCode;

    /** 结果提示信息（失败时为失败原因） */
    private String resultMsg;
}
