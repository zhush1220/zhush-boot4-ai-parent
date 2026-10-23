package com.zhush.ai.tools;

import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.websearch.WebSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 联网工具外壳测试：能力未装配时的降级回包、参数透传，以及工具名与授权脚本的一致性。
 *
 * <p>工具名是跨文件契约——{@code agent_definition.tool_names} 按名授权（见 sql/33_ai_web_search.sql），
 * 这里把 {@code web_search}/{@code web_fetch} 钉住：改名会有用例失败，而不是授权静默失效。
 */
class AgentWebSearchToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSearchService webSearchService = mock(WebSearchService.class);

    @SuppressWarnings("unchecked")
    private AgentWebSearchTools tools(WebSearchService service) {
        ObjectProvider<WebSearchService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        return new AgentWebSearchTools(provider);
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }

    @Test
    void missingCapabilityShouldReturnReadableDegradedJson() {
        AgentWebSearchTools tools = tools(null);
        AgentContext ctx = AgentContext.builder().sessionId("s-1").userId(9L).build();

        JsonNode search = parse(tools.webSearch("厦门 天气", 5, ctx));
        JsonNode fetch = parse(tools.webFetch("https://example.com", 1000, ctx));

        // 与 WebSearchService 的降级结构同形：ok=false + reason + hint，模型与前端都按这套字段处理
        assertThat(search.path("ok").asBoolean()).isFalse();
        assertThat(search.path("reason").asString("")).isEqualTo("联网搜索能力未开启");
        assertThat(search.path("hint").asString("")).contains("未能联网");
        assertThat(fetch.path("ok").asBoolean()).isFalse();
        assertThat(fetch.path("reason").asString("")).isEqualTo("联网搜索能力未开启");
    }

    @Test
    void shouldDelegateToServiceWithSameArguments() {
        AgentContext ctx = AgentContext.builder().sessionId("s-1").userId(9L).webSearchEnabled(true).build();
        when(webSearchService.searchJson("厦门 天气", 3, ctx)).thenReturn("{\"ok\":true,\"count\":1}");
        when(webSearchService.fetchJson("https://example.com", 8000, ctx)).thenReturn("{\"ok\":true}");

        assertThat(tools(webSearchService).webSearch("厦门 天气", 3, ctx)).isEqualTo("{\"ok\":true,\"count\":1}");
        assertThat(tools(webSearchService).webFetch("https://example.com", 8000, ctx)).isEqualTo("{\"ok\":true}");

        verify(webSearchService).searchJson("厦门 天气", 3, ctx);
        verify(webSearchService).fetchJson("https://example.com", 8000, ctx);
    }

    @Test
    void toolNamesShouldMatchGrantedToolNames() throws Exception {
        Method search = AgentWebSearchTools.class
                .getMethod("webSearch", String.class, Integer.class, AgentContext.class);
        Method fetch = AgentWebSearchTools.class
                .getMethod("webFetch", String.class, Integer.class, AgentContext.class);

        assertThat(search.getAnnotation(AgentTool.class).name()).isEqualTo("web_search");
        assertThat(fetch.getAnnotation(AgentTool.class).name()).isEqualTo("web_fetch");
        // 描述里必须提醒模型不要编造来源，这是引用可信度的唯一抓手
        assertThat(search.getAnnotation(AgentTool.class).description()).contains("不要凭记忆编造来源");
    }
}
