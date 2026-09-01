package com.zhush.agentscope.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话上下文：注入系统提示词与工具执行环境的用户/租户信息。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID（多租户隔离键）
     */
    private String tenantId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型：admin-管理端 member-会员端
     */
    private String userType;

    /**
     * 用户名称
     */
    private String nickname;
}
