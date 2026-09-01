package com.zhush.agentscope.safety;

import tools.jackson.databind.ObjectMapper;
import com.zhush.redis.core.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 工具审批处理器默认实现：待审批任务写入 Redis（key: agentscope:approval:{approvalId}），
 * TTL 自动回收，供管理端 REST 接口查询 / 审批 / 驳回。
 *
 * @author zhush
 */
public class DefaultToolApprovalHandler implements ToolApprovalHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultToolApprovalHandler.class);

    private static final String KEY_PREFIX = "agentscope:approval:";

    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public DefaultToolApprovalHandler(RedisCache redisCache, ObjectMapper objectMapper, Duration ttl) {
        this.redisCache = redisCache;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public ApprovalStatus submit(ApprovalRequest request) {
        if (request == null) {
            return ApprovalStatus.REJECTED;
        }
        String approvalId = request.getApprovalId();
        if (approvalId == null || approvalId.isEmpty()) {
            approvalId = UUID.randomUUID().toString().replace("-", "");
            request.setApprovalId(approvalId);
        }
        if (request.getExpireTime() == null) {
            request.setExpireTime(Instant.now().plus(ttl));
        }
        ApprovalRecord record = ApprovalRecord.of(request, ApprovalStatus.PENDING);
        persist(approvalId, record);
        log.info("提交工具审批, approvalId={}, tool={}, sessionId={}", approvalId, request.getToolName(),
                request.getSessionId());
        return ApprovalStatus.PENDING;
    }

    @Override
    public void approve(String approvalId, Map<String, Object> modifiedArgs) {
        ApprovalRecord record = load(approvalId);
        if (record == null) {
            log.warn("审批记录不存在, approvalId={}", approvalId);
            return;
        }
        if (record.getStatus() != ApprovalStatus.PENDING) {
            log.warn("审批状态非 PENDING, 忽略, approvalId={}, status={}", approvalId, record.getStatus());
            return;
        }
        if (modifiedArgs != null && !modifiedArgs.isEmpty()) {
            record.getRequest().setArguments(modifiedArgs);
        }
        record.setStatus(ApprovalStatus.APPROVED);
        persist(approvalId, record);
        log.info("审批通过, approvalId={}", approvalId);
    }

    @Override
    public void reject(String approvalId, String reason) {
        ApprovalRecord record = load(approvalId);
        if (record == null) {
            log.warn("审批记录不存在, approvalId={}", approvalId);
            return;
        }
        if (record.getStatus() != ApprovalStatus.PENDING) {
            log.warn("审批状态非 PENDING, 忽略, approvalId={}, status={}", approvalId, record.getStatus());
            return;
        }
        record.setStatus(ApprovalStatus.REJECTED);
        persist(approvalId, record);
        log.info("审批驳回, approvalId={}, reason={}", approvalId, reason);
    }

    /**
     * 查询审批记录（管理端 REST 使用）。
     *
     * @param approvalId 审批编号
     * @return 记录，不存在返回 null
     */
    public ApprovalRecord load(String approvalId) {
        try {
            String json = redisCache.get(key(approvalId));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, ApprovalRecord.class);
        } catch (Exception e) {
            log.warn("审批记录加载失败, approvalId={}, error={}", approvalId, e.getMessage());
            return null;
        }
    }

    private void persist(String approvalId, ApprovalRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            Duration remaining = Duration.between(Instant.now(), record.getRequest().getExpireTime());
            if (remaining.isNegative() || remaining.isZero()) {
                remaining = Duration.ofMinutes(5);
            }
            redisCache.set(key(approvalId), json, remaining);
        } catch (Exception e) {
            log.warn("审批记录保存失败, approvalId={}, error={}", approvalId, e.getMessage());
        }
    }

    private String key(String approvalId) {
        return KEY_PREFIX + approvalId;
    }

    /**
     * Redis 存储载体：请求 + 状态。
     */
    public static class ApprovalRecord {

        private ApprovalRequest request;

        private ApprovalStatus status;

        public ApprovalRecord() {
        }

        public ApprovalRecord(ApprovalRequest request, ApprovalStatus status) {
            this.request = request;
            this.status = status;
        }

        public static ApprovalRecord of(ApprovalRequest request, ApprovalStatus status) {
            return new ApprovalRecord(request, status);
        }

        public ApprovalRequest getRequest() {
            return request;
        }

        public void setRequest(ApprovalRequest request) {
            this.request = request;
        }

        public ApprovalStatus getStatus() {
            return status;
        }

        public void setStatus(ApprovalStatus status) {
            this.status = status;
        }
    }
}
