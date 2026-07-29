package com.zhush.oauth2.core;

/**
 * OAuth2 开放平台 scope 常量
 *
 * <p>基础 scope 之外的资源类 scope 按 {@code 域:操作} 命名（如 {@code member:read}），
 * 由各业务模块在客户端注册时分配、接口上用 {@link com.zhush.oauth2.annotation.OpenApiScope} 声明。
 *
 * @author zhush
 */
public final class OAuth2Scopes {

    private OAuth2Scopes() {
    }

    /**
     * 仅返回用户唯一标识
     */
    public static final String OPENID = "openid";

    /**
     * 昵称、头像等基础资料
     */
    public static final String USERINFO = "userinfo";

    /**
     * 用户角色列表（管理端账号体系）
     */
    public static final String ROLES = "roles";

}
