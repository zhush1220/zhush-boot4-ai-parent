package com.zhush.protection.idempotent.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.utils.json.JacksonUtils;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.protection.idempotent.core.IdempotentUserResolver;
import com.zhush.protection.idempotent.enums.IdempotentDimension;
import com.zhush.redis.core.RedisCache;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

/**
 * 幂等性切面
 *
 * <p>拦截标注 {@link Idempotent} 的方法，在执行前基于 Redis {@code SET NX} 抢占幂等标记：
 * <ol>
 *     <li>根据去重维度（参数/用户/接口）构建唯一 key</li>
 *     <li>{@code SET NX} 抢占成功 → 放行执行业务；失败（窗口内已存在）→ 判定为重复请求，
 *     抛出 {@code REPEATED_REQUEST} 业务错误，不执行业务逻辑</li>
 *     <li>业务执行抛异常时，按 {@link Idempotent#clearOnFailure()} 决定是否清除标记，
 *     默认清除以允许用户修正后重新提交</li>
 * </ol>
 *
 * @author zhush
 */
@Slf4j
@Aspect
public class IdempotentAspect {

    /**
     * 幂等 key 统一前缀
     */
    private static final String KEY_PREFIX = "zhush:idempotent:";

    private final RedisCache redisCache;
    private final IdempotentUserResolver userResolver;

    public IdempotentAspect(RedisCache redisCache, IdempotentUserResolver userResolver) {
        this.redisCache = redisCache;
        this.userResolver = userResolver;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint, idempotent);
        Duration window = Duration.ofMillis(idempotent.timeUnit().toMillis(idempotent.timeout()));

        // SET NX 抢占幂等标记：false 表示窗口内已存在 → 重复请求
        boolean acquired = redisCache.setIfAbsent(key, "1", window);
        if (!acquired) {
            log.warn("[Idempotent] 重复请求被拦截, key={}", key);
            throw new ServiceException(GlobalErrorCode.REPEATED_REQUEST.getCode(), idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 业务失败：按配置决定是否清除幂等标记（默认清除，允许重新提交）
            if (idempotent.clearOnFailure()) {
                redisCache.delete(key);
            }
            throw throwable;
        }
    }

    /**
     * 构建幂等 key：前缀 + MD5(接口标识 + 维度附加信息)
     */
    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        StringBuilder raw = new StringBuilder();
        raw.append(resolveInterfaceId(joinPoint));

        // 用户维度 / 参数维度都附加当前用户标识
        if (idempotent.dimension() != IdempotentDimension.INTERFACE) {
            raw.append(":user=").append(userResolver.currentUserId());
        }

        // 参数维度额外附加参数指纹 + 自定义 key
        if (idempotent.dimension() == IdempotentDimension.ARGS) {
            raw.append(":args=").append(argsFingerprint(joinPoint.getArgs()));
        }

        if (StrUtil.isNotBlank(idempotent.key())) {
            raw.append(":key=").append(idempotent.key());
        }
        return KEY_PREFIX + DigestUtil.md5Hex(raw.toString());
    }

    /**
     * 接口标识：优先取当前请求 URI，非 Web 环境降级为 类全名#方法名
     */
    private String resolveInterfaceId(ProceedingJoinPoint joinPoint) {
        HttpServletRequest request = currentRequest();
        if (request != null && StrUtil.isNotBlank(request.getRequestURI())) {
            return request.getRequestURI() + ":" + request.getMethod();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return joinPoint.getTarget().getClass().getName() + "#" + method.getName();
    }

    /**
     * 参数指纹：序列化可序列化入参后取 MD5（过滤 Servlet/文件流等不可序列化参数）
     */
    private String argsFingerprint(Object[] args) {
        if (args == null || args.length == 0) {
            return "empty";
        }
        Object[] recordable = Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof HttpSession || arg instanceof MultipartFile
                        || arg instanceof org.springframework.validation.BindingResult))
                .toArray();
        try {
            return DigestUtil.md5Hex(JacksonUtils.toJsonString(recordable));
        } catch (Exception e) {
            // 序列化失败时降级为参数个数，避免影响主流程
            log.warn("[Idempotent] 参数序列化失败，降级为按参数个数生成指纹", e);
            return String.valueOf(recordable.length);
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

}
