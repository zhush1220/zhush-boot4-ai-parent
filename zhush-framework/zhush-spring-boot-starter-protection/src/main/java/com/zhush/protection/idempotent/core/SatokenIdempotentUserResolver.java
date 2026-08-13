package com.zhush.protection.idempotent.core;

import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;

/**
 * 基于 Sa-Token 多账号体系的当前用户解析器
 *
 * <p>优先识别管理端登录态，其次会员端，格式为 {@code 账号类型:登录ID}
 * （如 {@code admin:1}、{@code member:2}），均未登录时返回 {@code anonymous}。
 *
 * @author zhush
 */
public class SatokenIdempotentUserResolver implements IdempotentUserResolver {

    @Override
    public String currentUserId() {
        try {
            Long adminId = StpAdminUtil.getLoginIdDefaultNull();
            if (adminId != null) {
                return LoginTypeConstants.ADMIN + ":" + adminId;
            }
            Long memberId = StpMemberUtil.getLoginIdDefaultNull();
            if (memberId != null) {
                return LoginTypeConstants.MEMBER + ":" + memberId;
            }
        } catch (Throwable ignored) {
            // 认证上下文不可用时按匿名处理，不阻断业务
        }
        return ANONYMOUS;
    }

}
