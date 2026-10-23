package com.zhush.ai.tools;

import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.tool.ToolParam;
import com.zhush.agentscope.websearch.WebSearchService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 联网搜索业务工具（L1 搜索 / L2 抓取）。
 *
 * <p>定位：AI 智能助手的出网执行层，由对话大模型在 ReAct 循环中按意图自主调用。与 Harness 内置
 * {@code web_search}/{@code web_fetch} 的区别是——内置工具由 builder 追加，绕过审批门、不计用量、
 * 不落审计、密钥只读进程环境变量；本类经 {@code @AgentTool} 注册，天然进入
 * {@code ReflectiveAgentTool} + 审批门 + 工具审计链路，密钥走 Spring 配置（方案 2.4）。
 *
 * <p>降级口径：{@code zhush.agentscope.web-search} 未开启或缺密钥时 {@link WebSearchService} Bean
 * 缺席，工具返回「能力未开启」的 JSON 说明而不是抛错——联网失败不得打断对话主链路。
 *
 * <p>本轮开关落在 {@link AgentContext#getWebSearchEnabled()}：工具集按智能体缓存、不能按轮次摘除，
 * 故由服务层在调用时判定，关闭时同样返回可读说明。
 *
 * @author zhush
 */
@Component
public class AgentWebSearchTools {

    /**
     * 能力未装配（开关关闭/无密钥）时的回包：与 {@code WebSearchService} 的降级结构同形，
     * 前端与模型都按 {@code ok=false} + {@code hint} 处理。
     */
    private static final String UNAVAILABLE_JSON = "{\"ok\":false,\"reason\":\"联网搜索能力未开启\","
            + "\"hint\":\"联网能力当前不可用，请直接基于已有知识回答，并向用户说明未能联网\"}";

    private final ObjectProvider<WebSearchService> webSearchServiceProvider;

    public AgentWebSearchTools(ObjectProvider<WebSearchService> webSearchServiceProvider) {
        this.webSearchServiceProvider = webSearchServiceProvider;
    }

    /**
     * 联网搜索。
     */
    @AgentTool(name = "web_search",
            description = "联网搜索实时/外部信息（新闻、价格、政策、版本发布、天气等模型知识覆盖不到的内容），"
                    + "返回带标题、链接、摘要、来源的结果列表；需要外部事实时调用，不要凭记忆编造来源")
    public String webSearch(
            @ToolParam(name = "query", description = "检索词（简洁、含关键实体，如「厦门 今天 天气」）") String query,
            @ToolParam(name = "maxResults", description = "返回条数，1~10，可空取默认 5") Integer maxResults,
            AgentContext ctx) {
        WebSearchService service = webSearchServiceProvider.getIfAvailable();
        if (service == null) {
            return UNAVAILABLE_JSON;
        }
        return service.searchJson(query, maxResults, ctx);
    }

    /**
     * 网页抓取。
     */
    @AgentTool(name = "web_fetch",
            description = "抓取指定 http(s) 网页并返回正文纯文本；用户直接给出链接、"
                    + "或搜索结果摘要不足以回答问题时调用（不支持需要 JS 渲染的页面）")
    public String webFetch(
            @ToolParam(name = "url", description = "目标网页绝对地址") String url,
            @ToolParam(name = "maxChars", description = "正文最大字符数，可空取默认 8000") Integer maxChars,
            AgentContext ctx) {
        WebSearchService service = webSearchServiceProvider.getIfAvailable();
        if (service == null) {
            return UNAVAILABLE_JSON;
        }
        return service.fetchJson(url, maxChars, ctx);
    }
}
