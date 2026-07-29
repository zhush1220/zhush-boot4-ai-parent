package com.zhush.oauth2.core;

import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.data.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.template.SaOAuth2Util;
import com.zhush.oauth2.annotation.OpenApiScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放 API 鉴权拦截器（注册到 {@code /open/**}）
 *
 * <p>校验链路：Bearer 令牌提取 → 令牌有效性（access_token / client_token 二选一）→
 * 客户端启用状态 → {@link OpenApiScope} scope 与调用级别校验 → 限流 → 写入
 * {@link OpenApiContext} 放行；请求完成后异步记录调用日志并清理上下文。
 *
 * <p>协议层错误按 OAuth2 规范返回 {@code 401/403/429} + {@code error}/{@code error_description}。
 *
 * @author zhush
 */
@Slf4j
public class OpenApiInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ATTR_START_TIME = "OPEN_API_START_TIME";
    private static final String ATTR_ERROR_CODE = "OPEN_API_ERROR_CODE";

    /**
     * 调用日志记录器（可选，业务侧提供实现）
     */
    private final OpenApiLogRecorder logRecorder;

    /**
     * 限流器（可选，业务侧提供实现）
     */
    private final OpenApiRateLimiter rateLimiter;

    public OpenApiInterceptor(OpenApiLogRecorder logRecorder, OpenApiRateLimiter rateLimiter) {
        this.logRecorder = logRecorder;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());

        // 1. 提取 Authorization: Bearer <token>
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return reject(request, response, 401, "invalid_request", "缺少 Authorization: Bearer 令牌");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();

        // 2. 令牌有效性：先按用户级 access_token 解析，再按应用级 client_token 解析
        String clientId;
        Long loginId = null;
        List<String> scopes;
        boolean userLevel;
        AccessTokenModel accessToken = SaOAuth2Util.getAccessToken(token);
        if (accessToken != null) {
            clientId = accessToken.getClientId();
            loginId = accessToken.getLoginId() == null ? null : Long.valueOf(accessToken.getLoginId().toString());
            scopes = accessToken.getScopes();
            userLevel = true;
        } else {
            ClientTokenModel clientToken = SaOAuth2Util.getClientToken(token);
            if (clientToken == null) {
                return reject(request, response, 401, "invalid_token", "令牌无效、已过期或已撤销");
            }
            clientId = clientToken.getClientId();
            scopes = clientToken.getScopes();
            userLevel = false;
        }
        // 令牌解析成功即写入上下文，使后续拒绝分支的调用日志也能关联 clientId
        OpenApiContext.set(new OpenApiContext.Caller(clientId, loginId, scopes, userLevel));

        // 3. 客户端启用状态（停用/删除的客户端 DataLoader 返回 null，存量令牌立即失效）
        if (SaOAuth2Util.getClientModel(clientId) == null) {
            return reject(request, response, 401, "invalid_client", "客户端已停用");
        }

        // 4. scope 与调用级别校验（方法注解优先于类注解）
        OpenApiScope scopeAnn = handlerMethod.getMethodAnnotation(OpenApiScope.class);
        if (scopeAnn == null) {
            scopeAnn = handlerMethod.getBeanType().getAnnotation(OpenApiScope.class);
        }
        if (scopeAnn != null) {
            if (scopeAnn.requireUser() && (!userLevel || loginId == null)) {
                return reject(request, response, 403, "insufficient_scope", "该接口仅支持用户级令牌调用");
            }
            for (String required : scopeAnn.value()) {
                if (scopes == null || !scopes.contains(required)) {
                    return reject(request, response, 403, "insufficient_scope", "令牌缺少所需 scope：" + required);
                }
            }
        }

        // 5. 限流/配额校验
        if (rateLimiter != null && !rateLimiter.tryAcquire(clientId)) {
            response.setHeader("Retry-After", "1");
            return reject(request, response, 429, "rate_limit_exceeded", "调用频率超限，请稍后重试");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            recordLog(request, response);
        } finally {
            OpenApiContext.clear();
        }
    }

    // ================ 内部方法 ================

    /**
     * 拒绝请求：按 OAuth2 规范输出错误响应，并记录失败调用日志
     */
    private boolean reject(HttpServletRequest request, HttpServletResponse response,
                           int status, String error, String description) throws Exception {
        request.setAttribute(ATTR_ERROR_CODE, error);
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + error + "\",\"error_description\":\"" + description + "\"}");
        // preHandle 返回 false 时 afterCompletion 不会回调本拦截器，此处直接落日志并清理上下文
        try {
            recordLog(request, response);
        } finally {
            OpenApiContext.clear();
        }
        return false;
    }

    private void recordLog(HttpServletRequest request, HttpServletResponse response) {
        if (logRecorder == null) {
            return;
        }
        Object startTime = request.getAttribute(ATTR_START_TIME);
        if (startTime == null) {
            return;
        }
        try {
            OpenApiContext.Caller caller = OpenApiContext.get();
            OpenApiCallRecord record = new OpenApiCallRecord();
            if (caller != null) {
                record.setClientId(caller.clientId());
                record.setLoginId(caller.loginId());
            }
            record.setRequestMethod(request.getMethod());
            record.setRequestUri(request.getRequestURI());
            record.setStatus(response.getStatus());
            record.setErrorCode((String) request.getAttribute(ATTR_ERROR_CODE));
            record.setCostMillis(System.currentTimeMillis() - (Long) startTime);
            record.setClientIp(request.getRemoteAddr());
            record.setCallTime(LocalDateTime.now());
            logRecorder.record(record);
        } catch (Exception e) {
            log.warn("[OpenApi] 调用日志记录失败", e);
        }
    }

}
