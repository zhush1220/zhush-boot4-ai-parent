package com.zhush.ai.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiChatSessionEntity;
import com.zhush.ai.mapper.AiChatSessionMapper;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.common.exception.ServiceExceptionUtil;
import org.springframework.stereotype.Component;

import static com.zhush.ai.enums.AiErrorCode.CONVERSATION_NOT_FOUND;
import static com.zhush.ai.enums.AiErrorCode.SESSION_FORBIDDEN;

/**
 * 会话归属守卫：读改写会话前校验「当前登录人即会话所有者」，关闭「越权读取/删除他人会话」缺口。
 *
 * <p>判定口径：{@code user_id + user_type} 双维匹配才视为归属（admin 与 member 的自增 ID 空间独立，
 * 单靠 user_id 会串号）。{@code user_type=admin} 放行——管理端会话管理页需查看全站会话，
 * 其入口本身由 {@code @SaCheckPermission} 保护，不在此重复鉴权。
 *
 * @author zhush
 */
@Component
public class AgentChatOwnershipGuard {

    private static final String USER_TYPE_ADMIN = "admin";

    private final AiChatSessionMapper sessionMapper;
    private final AiSessionContextResolver contextResolver;

    public AgentChatOwnershipGuard(AiChatSessionMapper sessionMapper,
                                  AiSessionContextResolver contextResolver) {
        this.sessionMapper = sessionMapper;
        this.contextResolver = contextResolver;
    }

    /**
     * 断言当前登录人为该会话所有者，返回会话实体。
     *
     * @param sessionId 会话 ID
     * @throws com.zhush.common.exception.ServiceException 会话不存在或非归属
     */
    public AiChatSessionEntity assertSessionOwner(String sessionId) {
        AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (session == null) {
            throw ServiceExceptionUtil.exception(CONVERSATION_NOT_FOUND, sessionId);
        }
        AgentContext ctx = contextResolver.resolve();
        if (ctx != null && USER_TYPE_ADMIN.equals(ctx.getUserType())) {
            return session;
        }
        Long currentUserId = ctx == null ? null : ctx.getUserId();
        String currentUserType = ctx == null ? null : ctx.getUserType();
        boolean owner = currentUserId != null
                && currentUserId.equals(session.getUserId())
                && currentUserType != null && currentUserType.equals(session.getUserType());
        if (!owner) {
            throw ServiceExceptionUtil.exception(SESSION_FORBIDDEN);
        }
        return session;
    }
}
