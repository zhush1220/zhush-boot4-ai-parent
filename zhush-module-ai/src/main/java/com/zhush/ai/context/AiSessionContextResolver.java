package com.zhush.ai.context;

import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.context.SessionContextResolver;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import org.springframework.stereotype.Component;

/**
 * AI 会话上下文解析器：从 Sa-Token 登录态解析用户 / 用户类型 / 租户。
 *
 * <p>管理端登录取 {@link StpAdminUtil}，会员端取 {@link StpMemberUtil}，均未登录时返回匿名上下文。
 *
 * @author zhush
 */
@Component
public class AiSessionContextResolver implements SessionContextResolver {

    @Override
    public AgentContext resolve() {
        if (StpAdminUtil.isLogin()) {
            return AgentContext.builder()
                    .tenantId("0")
                    .userId(StpAdminUtil.getLoginIdAsLong())
                    .userType("admin")
                    .build();
        }
        if (StpMemberUtil.isLogin()) {
            return AgentContext.builder()
                    .tenantId("0")
                    .userId(StpMemberUtil.getLoginIdAsLong())
                    .userType("member")
                    .build();
        }
        return AgentContext.builder()
                .tenantId("0")
                .userId(0L)
                .userType("admin")
                .build();
    }
}
