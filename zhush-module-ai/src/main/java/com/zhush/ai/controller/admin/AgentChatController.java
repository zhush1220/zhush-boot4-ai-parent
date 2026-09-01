package com.zhush.ai.controller.admin;

import com.zhush.ai.param.AgentChatParam;
import com.zhush.ai.query.AiSessionQuery;
import com.zhush.ai.service.AgentChatService;
import com.zhush.ai.vo.AgentVO;
import com.zhush.ai.vo.AiChatMessageVO;
import com.zhush.ai.vo.AiSessionVO;
import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能助手接口（对话 / 流式 / 会话管理）。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-AI智能助手", description = "智能体对话、流式输出、会话管理")
@RequestMapping("/admin/ai/agent")
public class AgentChatController {

    private final AgentChatService chatService;
    private final AgentRegistry agentRegistry;

    public AgentChatController(AgentChatService chatService, AgentRegistry agentRegistry) {
        this.chatService = chatService;
        this.agentRegistry = agentRegistry;
    }

    /**
     * 同步对话
     */
    @PostMapping("/chat")
    @Operation(summary = "同步对话")
    public Result<com.zhush.agentscope.agent.AgentReply> chat(@RequestBody @Valid AgentChatParam param) {
        return Result.success(chatService.chat(param));
    }

    /**
     * 流式对话（SSE）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话（SSE）")
    public Flux<AgentEvent> stream(@RequestBody @Valid AgentChatParam param) {
        return chatService.stream(param);
    }

    /**
     * 智能体列表
     */
    @GetMapping("/list")
    @Operation(summary = "智能体列表")
    public Result<List<AgentVO>> list() {
        List<AgentVO> result = new ArrayList<>();
        for (AgentDefinition def : agentRegistry.all()) {
            AgentVO vo = new AgentVO();
            vo.setName(def.getName());
            vo.setDescription(def.getDescription());
            vo.setModel(def.getModel());
            vo.setStreaming(def.isStreaming() ? 1 : 0);
            vo.setMaxSteps(def.getMaxSteps());
            vo.setGroupCode(def.getGroupCode());
            result.add(vo);
        }
        return Result.success(result);
    }

    /**
     * 新建会话
     */
    @PostMapping("/session/new")
    @Operation(summary = "新建会话")
    public Result<String> newSession(@RequestBody @Valid AgentChatParam param) {
        return Result.success(chatService.newSession(param.getAgentName(), param.getMessage()));
    }

    /**
     * 会话分页
     */
    @GetMapping("/session/page")
    @Operation(summary = "会话分页")
    public Result<PageResult<AiSessionVO>> sessionPage(@Valid AiSessionQuery query) {
        return Result.success(chatService.sessionPage(query));
    }

    /**
     * 会话消息
     */
    @GetMapping("/session/{sessionId}/messages")
    @Operation(summary = "会话消息")
    public Result<List<AiChatMessageVO>> messages(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        return Result.success(chatService.sessionMessages(sessionId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话")
    public Result<Void> delete(@Parameter(description = "会话ID") @PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success();
    }
}
