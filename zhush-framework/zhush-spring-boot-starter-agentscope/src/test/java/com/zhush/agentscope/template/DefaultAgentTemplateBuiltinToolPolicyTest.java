package com.zhush.agentscope.template;

import com.zhush.agentscope.config.AgentScopeProperties;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 内置工具收敛（方案 P0）装配测试：AgentScope 的 {@code web_search}/{@code web_fetch}/{@code execute}/文件工具
 * 由 builder 无条件追加，绕过项目的审批门、敏感词与用量口径，故默认必须三项全关。
 *
 * <p>这条同时是安全回归守卫：日后升级 AgentScope 若有人把默认值改成「开」，本用例即失败。
 */
class DefaultAgentTemplateBuiltinToolPolicyTest {

    private final HarnessAgent.Builder builder = mock(HarnessAgent.Builder.class);

    @Test
    void defaultPolicyShouldDisableWebShellAndFilesystem() {
        DefaultAgentTemplate.applyBuiltinToolPolicy(builder, new AgentScopeProperties.WebSearch.BuiltinTools());

        verify(builder).disableWebTools();
        verify(builder).disableShellTool();
        verify(builder).disableFilesystemTools();
    }

    @Test
    void keepingWebToolsShouldStillDisableShellAndFilesystem() {
        AgentScopeProperties.WebSearch.BuiltinTools policy = new AgentScopeProperties.WebSearch.BuiltinTools();
        policy.setDisableWeb(false);

        DefaultAgentTemplate.applyBuiltinToolPolicy(builder, policy);

        verify(builder, never()).disableWebTools();
        verify(builder).disableShellTool();
        verify(builder).disableFilesystemTools();
    }

    @Test
    void keepingShellToolShouldBeAnExplicitSeparateDecision() {
        AgentScopeProperties.WebSearch.BuiltinTools policy = new AgentScopeProperties.WebSearch.BuiltinTools();
        policy.setDisableShell(false);

        DefaultAgentTemplate.applyBuiltinToolPolicy(builder, policy);

        verify(builder).disableWebTools();
        verify(builder, never()).disableShellTool();
        verify(builder).disableFilesystemTools();
    }

    @Test
    void keepingFilesystemToolShouldBeAnExplicitSeparateDecision() {
        AgentScopeProperties.WebSearch.BuiltinTools policy = new AgentScopeProperties.WebSearch.BuiltinTools();
        policy.setDisableFilesystem(false);

        DefaultAgentTemplate.applyBuiltinToolPolicy(builder, policy);

        verify(builder).disableWebTools();
        verify(builder).disableShellTool();
        verify(builder, never()).disableFilesystemTools();
    }
}
