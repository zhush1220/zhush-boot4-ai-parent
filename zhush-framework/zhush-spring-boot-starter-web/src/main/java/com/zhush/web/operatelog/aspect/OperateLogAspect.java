package com.zhush.web.operatelog.aspect;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.response.Result;
import com.zhush.common.utils.json.JacksonUtils;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.core.OperateLogHandler;
import com.zhush.web.operatelog.core.OperateLogRecord;
import com.zhush.web.util.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 *
 * <p>拦截标注 {@link OperateLog} 的方法，记录操作数据的整个过程：
 * <ol>
 *     <li>执行前：采集操作人请求信息（IP/UA/URL/请求方式）、目标方法与入参、开始时间</li>
 *     <li>执行后：采集返回结果、结果码与耗时</li>
 *     <li>异常时：采集异常码与异常信息（记录完成后异常继续向上抛出，不影响原有流程）</li>
 * </ol>
 * 采集完成后交由 {@link OperateLogHandler} 处理（业务侧落库/上报）；
 * 日志处理自身的异常会被兜底捕获，绝不影响业务主流程。
 *
 * @author zhush
 */
@Slf4j
@Aspect
public class OperateLogAspect {

    /**
     * 入参 JSON 最大长度（超出截断，与 sys_operate_log 表字段长度对应）
     */
    private static final int MAX_ARGS_LENGTH = 2000;

    /**
     * 返回结果 JSON 最大长度（超出截断）
     */
    private static final int MAX_RESULT_LENGTH = 4000;

    /**
     * 结果提示信息最大长度（超出截断）
     */
    private static final int MAX_MSG_LENGTH = 512;

    private final ObjectProvider<OperateLogHandler> handlerProvider;

    public OperateLogAspect(ObjectProvider<OperateLogHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;
    }

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        OperateLogRecord record = new OperateLogRecord();
        record.setStartTime(LocalDateTime.now());
        long start = System.currentTimeMillis();

        // 1. 执行前采集：请求信息 + 方法信息 + 入参
        fillBaseInfo(record, joinPoint, operateLog);

        try {
            // 2. 执行目标方法
            Object result = joinPoint.proceed();
            fillSuccessResult(record, operateLog, result);
            return result;
        } catch (Throwable throwable) {
            // 3. 异常采集后原样抛出，交由全局异常处理器处理
            fillErrorResult(record, throwable);
            throw throwable;
        } finally {
            record.setDuration((int) (System.currentTimeMillis() - start));
            dispatch(record);
        }
    }

    /**
     * 采集请求信息、目标方法与入参
     */
    private void fillBaseInfo(OperateLogRecord record, ProceedingJoinPoint joinPoint, OperateLog operateLog) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            record.setModule(resolveModule(operateLog, joinPoint));
            record.setName(resolveName(operateLog, method));
            record.setType(operateLog.type().getType());
            record.setJavaMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName());
            // 链路追踪 ID：从 SkyWalking 上下文读取，无 agent/无链路时返回 "N/A"，避免落库空值
            record.setTraceId(StrUtil.blankToDefault(TraceContext.traceId(), "N/A"));
            if (operateLog.recordParams()) {
                record.setJavaMethodArgs(buildArgs(joinPoint.getArgs()));
            }

            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                record.setRequestMethod(request.getMethod());
                record.setRequestUrl(request.getRequestURI());
                record.setUserIp(ServletUtils.getClientIp(request));
                record.setUserAgent(StrUtil.maxLength(ServletUtils.getUserAgent(request), MAX_MSG_LENGTH));
            }
        } catch (Exception e) {
            log.warn("[OperateLog] 采集请求信息失败", e);
        }
    }

    /**
     * 采集成功结果：结果码取统一返回 {@link Result} 的 code，其余类型视为成功
     */
    private void fillSuccessResult(OperateLogRecord record, OperateLog operateLog, Object result) {
        if (result instanceof Result<?> r) {
            record.setResultCode(r.getCode());
            record.setResultMsg(StrUtil.maxLength(r.getMessage(), MAX_MSG_LENGTH));
        } else {
            record.setResultCode(GlobalErrorCode.SUCCESS.getCode());
        }
        if (operateLog.recordResult()) {
            record.setResultData(StrUtil.maxLength(toJson(result), MAX_RESULT_LENGTH));
        }
    }

    /**
     * 采集异常结果：业务异常取其错误码，其余按系统内部错误处理
     */
    private void fillErrorResult(OperateLogRecord record, Throwable throwable) {
        if (throwable instanceof ServiceException se) {
            record.setResultCode(se.getCode());
        } else {
            record.setResultCode(GlobalErrorCode.INTERNAL_ERROR.getCode());
        }
        record.setResultMsg(StrUtil.maxLength(throwable.getMessage(), MAX_MSG_LENGTH));
    }

    /**
     * 分发给业务处理器；未注册实现时仅打印日志
     */
    private void dispatch(OperateLogRecord record) {
        try {
            OperateLogHandler handler = handlerProvider.getIfAvailable();
            if (handler != null) {
                handler.handle(record);
            } else {
                log.info("[OperateLog] module={}, name={}, method={}, code={}, duration={}ms",
                        record.getModule(), record.getName(), record.getJavaMethod(),
                        record.getResultCode(), record.getDuration());
            }
        } catch (Exception e) {
            // 日志处理失败不影响业务主流程
            log.error("[OperateLog] 操作日志处理失败, record={}", record, e);
        }
    }

    /**
     * 模块名：注解优先，为空取类上 Swagger {@code @Tag} 的 name
     */
    private String resolveModule(OperateLog operateLog, ProceedingJoinPoint joinPoint) {
        if (StrUtil.isNotBlank(operateLog.module())) {
            return operateLog.module();
        }
        Tag tag = joinPoint.getTarget().getClass().getAnnotation(Tag.class);
        return tag != null ? tag.name() : "";
    }

    /**
     * 操作名：注解优先，为空取方法上 Swagger {@code @Operation} 的 summary
     */
    private String resolveName(OperateLog operateLog, Method method) {
        if (StrUtil.isNotBlank(operateLog.name())) {
            return operateLog.name();
        }
        Operation operation = method.getAnnotation(Operation.class);
        return operation != null ? operation.summary() : "";
    }

    /**
     * 序列化方法入参（过滤 Servlet 对象、文件流等不可序列化参数）
     */
    private String buildArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        Object[] recordable = Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof HttpSession || arg instanceof MultipartFile
                        || arg instanceof BindingResult))
                .toArray();
        return StrUtil.maxLength(toJson(recordable), MAX_ARGS_LENGTH);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return JacksonUtils.toJsonString(obj);
        } catch (Exception e) {
            return StrUtil.toString(obj);
        }
    }

}
