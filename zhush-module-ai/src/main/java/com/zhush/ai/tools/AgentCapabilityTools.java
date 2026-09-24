package com.zhush.ai.tools;

import com.zhush.ai.generation.AgentDocumentService;
import com.zhush.ai.generation.FileArtifact;
import com.zhush.ai.generation.RemoteFileFetcher;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.tool.ToolParam;
import com.zhush.file.core.DocParseResult;
import com.zhush.file.core.DocParseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话多能力业务工具：文档生成（Markdown → Word/PDF）与文档解析（文件 → 纯文本）。
 *
 * <p>定位：对话框自然语言多能力的执行层，由 ai-chat 智能体在 ReAct 循环中按意图自主调用；
 * 工具返回 JSON 文本给 LLM，其中 {@code artifact} 字段会被
 * {@code AgentChatService} 从工具结果事件提取并落库 {@code ai_chat_message.attachments}，
 * 前端据此渲染下载卡片（历史回显同源）。
 *
 * <p>末位 {@link AgentContext} 形参不告知 LLM，由框架从 {@code RuntimeContext} 注入，
 * 用于把产物文件归属到真实用户（落 {@code infra_file.user_id/user_type}）。
 *
 * <p>{@code parseDocument} 读取文件时做 SSRF 防护，白名单逻辑统一收敛在
 * {@link RemoteFileFetcher}（仅允许已配置存储域名前缀的 URL 或多文件上传接口返回的相对路径）。
 *
 * @author zhush
 */
@Component
public class AgentCapabilityTools {

    private static final Logger log = LoggerFactory.getLogger(AgentCapabilityTools.class);

    /** 解析结果回给 LLM 的文本截断上限（字符），防超大文档撑爆上下文 */
    private static final int MAX_PARSE_CHARS = 6_000;

    private final AgentDocumentService documentService;
    private final DocParseService docParseService;
    private final RemoteFileFetcher remoteFileFetcher;
    private final ObjectMapper objectMapper;

    public AgentCapabilityTools(AgentDocumentService documentService,
                                DocParseService docParseService,
                                RemoteFileFetcher remoteFileFetcher,
                                ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.docParseService = docParseService;
        this.remoteFileFetcher = remoteFileFetcher;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成文档（Word/PDF）。
     */
    @AgentTool(name = "generateDocument",
            description = "将 Markdown 格式的正文渲染为 Word(docx) 或 PDF 文档并返回下载地址；"
                    + "用户要求导出/生成文档/下载 Word、PDF 时调用")
    public String generateDocument(
            @ToolParam(name = "content", description = "文档正文（Markdown 格式，含标题/段落/列表/表格结构）") String content,
            @ToolParam(name = "format", description = "输出格式：docx 或 pdf，缺省 docx") String format,
            @ToolParam(name = "fileName", description = "文件名（可不含扩展名）") String fileName,
            AgentContext ctx) {
        try {
            FileArtifact artifact = documentService.generate(content, format, fileName,
                    ctx == null ? null : ctx.getUserType(), ctx == null ? null : ctx.getUserId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("artifact", artifact);
            result.put("hint", "文档已生成，请将 artifact.name 以 Markdown 链接形式（[名称](artifact.url)）附在回复末尾");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[AgentCapabilityTools] generateDocument 失败", e);
            return failJson(e.getMessage());
        }
    }

    /**
     * 解析文档（提取纯文本）。
     */
    @AgentTool(name = "parseDocument",
            description = "解析已上传的文档（docx/doc/pdf/txt/html 等）为纯文本；"
                    + "用户要求总结/提炼/问答附件内容时调用，fileUrl 取消息中的附件地址")
    public String parseDocument(
            @ToolParam(name = "fileUrl", description = "文件地址（上传接口返回的 URL 或相对路径）") String fileUrl,
            @ToolParam(name = "fileName", description = "原始文件名（可选，辅助格式识别）") String fileName,
            AgentContext ctx) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return "{\"ok\":false,\"error\":\"文件地址不能为空，请先上传附件\"}";
        }
        try {
            String name = (fileName == null || fileName.isBlank()) ? guessName(fileUrl) : fileName;
            byte[] bytes = remoteFileFetcher.fetchFromStorage(fileUrl.trim());
            DocParseResult parsed = docParseService.parse(bytes, name);
            String text = parsed.getText() == null ? "" : parsed.getText().trim();
            if (text.isEmpty()) {
                return "{\"ok\":false,\"error\":\"未从文件中提取到文本内容\"}";
            }
            boolean truncated = text.length() > MAX_PARSE_CHARS;
            if (truncated) {
                text = text.substring(0, MAX_PARSE_CHARS) + "…（内容过长已截断）";
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("fileName", name);
            result.put("text", text);
            result.put("truncated", truncated);
            result.put("hint", "请基于 text 内容回答用户问题");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[AgentCapabilityTools] parseDocument 失败, url={}", fileUrl, e);
            return failJson(e.getMessage());
        }
    }

    // ================ 内部 ================

    private static String guessName(String fileUrl) {
        String path = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private String failJson(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("error", message == null ? "未知错误" : message);
        return objectMapper.writeValueAsString(result);
    }
}
