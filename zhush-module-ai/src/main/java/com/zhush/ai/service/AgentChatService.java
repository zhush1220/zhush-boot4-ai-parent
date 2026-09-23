package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.entity.AiChatSessionEntity;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.mapper.AiChatSessionMapper;
import com.zhush.ai.param.AgentChatParam;
import com.zhush.ai.query.AiSessionQuery;
import com.zhush.ai.vo.AiChatMessageVO;
import com.zhush.ai.vo.AiSessionVO;
import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentReply;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventType;
import com.zhush.agentscope.event.AgentFinishedEvent;
import com.zhush.agentscope.event.AgentResumeChannels;
import com.zhush.agentscope.event.AgentToolResultEvent;
import com.zhush.agentscope.template.AgentTemplate;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zhush.ai.enums.AiErrorCode.CONVERSATION_NOT_FOUND;
import static com.zhush.ai.enums.AiErrorCode.MESSAGE_CONTENT_EMPTY;

/**
 * AI 对话服务：统一封装 {@link AgentTemplate}，并把会话/消息持久化到 DB（ai_chat_session / ai_chat_message）。
 *
 * <p>会话状态主体由 starter 的 {@code AgentSessionManager}（Redis）承担，本服务承担面向用户的列表/历史落库。
 *
 * @author zhush
 */
@Service
public class AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_FINISHED = 2;
    private static final int STATUS_MESSAGE_NORMAL = 1;

    private final AgentTemplate agentTemplate;
    private final AgentRegistry agentRegistry;
    private final AiSessionContextResolver contextResolver;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final AgentResumeChannels resumeChannels;

    public AgentChatService(AgentTemplate agentTemplate, AgentRegistry agentRegistry,
                            AiSessionContextResolver contextResolver, AiChatSessionMapper sessionMapper,
                            AiChatMessageMapper messageMapper, ObjectMapper objectMapper,
                            AgentResumeChannels resumeChannels) {
        this.agentTemplate = agentTemplate;
        this.agentRegistry = agentRegistry;
        this.contextResolver = contextResolver;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
        this.resumeChannels = resumeChannels;
    }

    /**
     * 同步对话。
     */
    public AgentReply chat(AgentChatParam param) {
        if (param.getMessage() == null || param.getMessage().isBlank()) {
            throw ServiceExceptionUtil.exception(MESSAGE_CONTENT_EMPTY);
        }
        AgentContext ctx = contextResolver.resolve();
        String sessionId = param.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = agentTemplate.newSession(param.getAgentName());
        }
        ensureSessionRow(sessionId, param.getAgentName(), param.getMessage());
        AgentReply reply = agentTemplate.chat(param.getAgentName(), sessionId, param.getMessage());
        saveMessage(sessionId, "user", param.getMessage());
        // 同步路径的 Token 由 AgentReply 携带（与事件流同源），一并累加到会话累计消耗
        saveMessage(sessionId, "assistant", reply.getReply(),
                reply.getInputTokens(), reply.getOutputTokens(), reply.getTotalTokens(), null);
        return reply;
    }

    /**
     * 流式对话：返回事件流（SSE），消息落库在流结束时执行。
     */
    public Flux<AgentEvent> stream(AgentChatParam param) {
        AgentContext ctx = contextResolver.resolve();
        String sessionId = param.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = agentTemplate.newSession(param.getAgentName());
        }
        ensureSessionRow(sessionId, param.getAgentName(), param.getMessage());
        saveMessage(sessionId, "user", param.getMessage());
        final String finalSessionId = sessionId;
        // 产物收集：从 generateDocument 工具结果事件提取 artifact，随 assistant 消息落 attachments
        final List<String> artifacts = Collections.synchronizedList(new ArrayList<>());
        return agentTemplate.stream(param.getAgentName(), finalSessionId, param.getMessage())
                .doOnNext(event -> {
                    if (event instanceof AgentToolResultEvent toolResult) {
                        collectArtifact(artifacts, toolResult);
                    }
                    if (event.getEventType() == AgentEventType.FINISHED && event instanceof AgentFinishedEvent finishedEvent) {
                        saveMessage(finalSessionId, "assistant", finishedEvent.getReply(),
                                finishedEvent.getInputTokens(), finishedEvent.getOutputTokens(),
                                finishedEvent.getTotalTokens(), buildAttachments(artifacts));
                        updateSessionStatus(finalSessionId, STATUS_FINISHED);
                    } else if (event.getEventType() == AgentEventType.ERROR) {
                        updateSessionStatus(finalSessionId, 4);
                    }
                });
    }

    /**
     * 订阅会话续跑 SSE 通道（路线 B）：审批跨请求完成后，续跑增量经此通道流回前端。
     */
    public Flux<AgentEvent> resumeChannel(String sessionId) {
        return resumeChannels.subscribe(sessionId);
    }

    /**
     * 触发路线 B 原生续跑：审批后同会话 key 喂回 {@code ConfirmResult}，异步驱动续跑事件流，
     * 完成后落 {@code ai_chat_message} 并回写会话状态。事件推送由 starter 侧统一写入续跑通道。
     *
     * <p>fire-and-forget：不阻塞审批接口线程；异常仅记日志（已挂起的会话可由用户重新发起）。
     */
    public void resumeNative(String agentName, String sessionId, String replyId, boolean confirmed,
                             Map<String, Object> modifiedArgs) {
        final List<String> artifacts = Collections.synchronizedList(new ArrayList<>());
        agentTemplate.resumeConfirm(agentName, sessionId, replyId, confirmed, modifiedArgs)
                .subscribe(event -> {
                    if (event instanceof AgentToolResultEvent toolResult) {
                        collectArtifact(artifacts, toolResult);
                    }
                    if (event.getEventType() == AgentEventType.FINISHED && event instanceof AgentFinishedEvent finished) {
                        saveMessage(sessionId, "assistant", finished.getReply(),
                                finished.getInputTokens(), finished.getOutputTokens(), finished.getTotalTokens(),
                                buildAttachments(artifacts));
                        updateSessionStatus(sessionId, STATUS_FINISHED);
                    } else if (event.getEventType() == AgentEventType.ERROR) {
                        updateSessionStatus(sessionId, 4);
                    }
                }, err -> log.error("原生续跑异常, agent={}, sessionId={}, replyId={}", agentName, sessionId, replyId, err));
    }

    /**
     * 新建会话。
     */
    public String newSession(String agentName, String message) {
        String sessionId = agentTemplate.newSession(agentName);
        ensureSessionRow(sessionId, agentName, message);
        return sessionId;
    }

    /**
     * 会话分页。
     */
    public PageResult<AiSessionVO> sessionPage(AiSessionQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(AiChatSessionEntity::getAgentName, query.getAgentName())
                .eq(AiChatSessionEntity::getUserType, query.getUserType())
                .eq(AiChatSessionEntity::getUserId, query.getUserId())
                .eq(AiChatSessionEntity::getStatus, query.getStatus())
                .eq(AiChatSessionEntity::getTenantId, query.getTenantId());
        queryWrapper.orderBy("last_active_time", false);
        Page<AiChatSessionEntity> page = sessionMapper.selectPageX(query, queryWrapper);
        List<AiSessionVO> list = toSessionVOs(page.getRecords());
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 会话消息列表。
     */
    public List<AiChatMessageVO> sessionMessages(String sessionId) {
        List<AiChatMessageEntity> list = messageMapper.selectBySessionId(sessionId);
        List<AiChatMessageVO> result = new ArrayList<>(list.size());
        for (AiChatMessageEntity entity : list) {
            result.add(toMessageVO(entity));
        }
        return result;
    }

    /**
     * 删除会话。
     */
    public void deleteSession(String sessionId) {
        AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (session != null) {
            sessionMapper.deleteById(session.getId());
        }
        agentTemplate.clearSession(sessionId);
    }

    /**
     * 重命名会话标题。
     */
    public void renameSession(String sessionId, String title) {
        AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (session == null) {
            throw ServiceExceptionUtil.exception(CONVERSATION_NOT_FOUND, sessionId);
        }
        String newTitle = title.trim();
        if (newTitle.length() > 50) {
            newTitle = newTitle.substring(0, 50);
        }
        session.setTitle(newTitle);
        sessionMapper.update(session);
    }

    // ================ 内部方法 ================

    private void ensureSessionRow(String sessionId, String agentName, String firstMessage) {
        AiChatSessionEntity existing = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (existing != null) {
            existing.setLastActiveTime(LocalDateTime.now());
            sessionMapper.update(existing);
            return;
        }
        AgentDefinition def = agentRegistry.get(agentName);
        AiChatSessionEntity entity = new AiChatSessionEntity();
        entity.setSessionId(sessionId);
        entity.setAgentName(agentName);
        entity.setAgentDesc(def == null ? "" : def.getDescription());
        entity.setModel(def == null ? "" : (def.getModel() == null ? "" : def.getModel()));
        entity.setTitle(buildTitle(firstMessage));
        entity.setUserId(contextResolver.resolve().getUserId());
        entity.setUserType(contextResolver.resolve().getUserType());
        entity.setTenantId(0L);
        entity.setMessageCount(0);
        entity.setTokenCost(0L);
        entity.setStatus(STATUS_ACTIVE);
        entity.setStartTime(LocalDateTime.now());
        entity.setLastActiveTime(LocalDateTime.now());
        sessionMapper.insert(entity);
    }

    private void saveMessage(String sessionId, String role, String content) {
        saveMessage(sessionId, role, content, 0, 0, 0, null);
    }

    /**
     * 落库消息；assistant 消息携带 Token 用量时写入 {@code token_usage}，并累加会话 {@code token_cost}；
     * 可选 attachments 写入产物附件 JSON（对话生成文件回链）。
     *
     * <p>会话行此处按「读-改-写」更新（与 messageCount 同一处），用量日聚合表另由监听器原子累加，
     * 两者不双写同一列。
     */
    private void saveMessage(String sessionId, String role, String content,
                             int inputTokens, int outputTokens, int totalTokens, String attachments) {
        AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (session == null) {
            return;
        }
        int total = totalTokens > 0 ? totalTokens : Math.max(inputTokens, 0) + Math.max(outputTokens, 0);
        AiChatMessageEntity entity = new AiChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setSeq(session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
        entity.setRole(role);
        entity.setContentType("text");
        entity.setContent(content);
        entity.setAttachments(attachments);
        entity.setStatus(STATUS_MESSAGE_NORMAL);
        if (total > 0) {
            entity.setTokenUsage(buildTokenUsage(inputTokens, outputTokens, total));
            long cost = session.getTokenCost() == null ? 0L : session.getTokenCost();
            session.setTokenCost(cost + total);
        }
        messageMapper.insert(entity);
        session.setMessageCount(session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
        session.setLastActiveTime(LocalDateTime.now());
        sessionMapper.update(session);
    }

    /**
     * 从工具结果事件提取产物：仅处理成功执行的 generateDocument，
     * 结果 JSON 形如 {@code {"ok":true,"artifact":{...}}}；解析失败静默忽略（不影响主流程）。
     */
    private void collectArtifact(List<String> artifacts, AgentToolResultEvent event) {
        if (!event.isOk() || !"generateDocument".equals(event.getName())
                || event.getResult() == null || event.getResult().isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(event.getResult());
            JsonNode artifact = root.path("artifact");
            if (root.path("ok").asBoolean(false) && artifact.isObject()) {
                artifacts.add(artifact.toString());
            }
        } catch (JacksonException e) {
            log.debug("[collectArtifact] 工具结果非 JSON，忽略: {}", e.getMessage());
        }
    }

    /**
     * 产物列表 → attachments 列 JSON（无产物时保持 NULL）。
     */
    private String buildAttachments(List<String> artifacts) {
        if (artifacts.isEmpty()) {
            return null;
        }
        synchronized (artifacts) {
            return "[" + String.join(",", artifacts) + "]";
        }
    }

    /**
     * Token 用量 JSON（列类型为 JSON，供应商未回传时不写入即保持 NULL，不写 0 值误导统计）。
     */
    private String buildTokenUsage(int inputTokens, int outputTokens, int totalTokens) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("input", Math.max(inputTokens, 0));
        usage.put("output", Math.max(outputTokens, 0));
        usage.put("total", totalTokens);
        try {
            return objectMapper.writeValueAsString(usage);
        } catch (JacksonException e) {
            return null;
        }
    }

    private void updateSessionStatus(String sessionId, int status) {
        AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
        if (session != null) {
            session.setStatus(status);
            if (status == STATUS_FINISHED) {
                session.setFinishTime(LocalDateTime.now());
            }
            sessionMapper.update(session);
        }
    }

    private String buildTitle(String message) {
        if (message == null) {
            return "新会话";
        }
        return message.length() > 20 ? message.substring(0, 20) : message;
    }

    private List<AiSessionVO> toSessionVOs(List<AiChatSessionEntity> entities) {
        List<AiSessionVO> result = new ArrayList<>(entities.size());
        for (AiChatSessionEntity entity : entities) {
            AiSessionVO vo = new AiSessionVO();
            vo.setId(entity.getId());
            vo.setSessionId(entity.getSessionId());
            vo.setAgentName(entity.getAgentName());
            vo.setAgentDesc(entity.getAgentDesc());
            vo.setModel(entity.getModel());
            vo.setTitle(entity.getTitle());
            vo.setUserId(entity.getUserId());
            vo.setUserType(entity.getUserType());
            vo.setMessageCount(entity.getMessageCount());
            vo.setTokenCost(entity.getTokenCost());
            vo.setStatus(entity.getStatus());
            vo.setStartTime(entity.getStartTime());
            vo.setFinishTime(entity.getFinishTime());
            vo.setLastActiveTime(entity.getLastActiveTime());
            result.add(vo);
        }
        return result;
    }

    private AiChatMessageVO toMessageVO(AiChatMessageEntity entity) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setSessionId(entity.getSessionId());
        vo.setSeq(entity.getSeq());
        vo.setRole(entity.getRole());
        vo.setContentType(entity.getContentType());
        vo.setContent(entity.getContent());
        vo.setThinking(entity.getThinking());
        vo.setToolCalls(entity.getToolCalls());
        vo.setAttachments(entity.getAttachments());
        vo.setTokenUsage(entity.getTokenUsage());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
