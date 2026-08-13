package com.zhush.protection.idempotent.core;

/**
 * 幂等场景下的当前用户解析器
 *
 * <p>用于「用户维度 / 参数维度」去重时识别当前请求的登录人，
 * 将其并入幂等 key，使不同用户的相同请求互不影响。
 * 未接入认证框架或当前为匿名请求时，返回固定值 {@code anonymous}。
 *
 * @author zhush
 */
public interface IdempotentUserResolver {

    /**
     * 匿名用户标识
     */
    String ANONYMOUS = "anonymous";

    /**
     * 解析当前登录用户标识（建议带账号类型前缀，如 {@code admin:1}、{@code member:2}）；
     * 未登录或无法识别时返回 {@link #ANONYMOUS}
     */
    String currentUserId();

}
