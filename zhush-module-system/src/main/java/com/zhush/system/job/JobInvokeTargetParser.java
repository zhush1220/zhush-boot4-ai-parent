package com.zhush.system.job;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.utils.json.JacksonUtils;
import com.zhush.system.entity.SysJob;
import com.zhush.system.enums.SystemErrorCode;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 定时任务调用目标解析器
 *
 * <p>调用目标格式为 {@code beanName.method()}，对应 Spring 容器中的 Bean 方法；
 * 任务参数（{@code sys_job.params}，JSON 对象）按方法形参名匹配后转换传入：
 * <ul>
 *     <li>无参方法：参数必须为空</li>
 *     <li>单形参方法：整个参数对象/字符串转换后传入</li>
 *     <li>多形参方法：按形参名从参数对象中取值逐一转换（依赖 {@code -parameters} 编译参数）</li>
 * </ul>
 *
 * @author zhush
 */
@Component
public class JobInvokeTargetParser {

    /**
     * JobDataMap 中任务ID的键
     */
    public static final String JOB_ID_KEY = "jobId";

    private final ApplicationContext applicationContext;

    public JobInvokeTargetParser(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 校验调用目标合法性（新增/修改任务时预校验）：
     * Bean 存在、存在形参数匹配的方法、参数为合法 JSON 对象
     */
    public void validate(String invokeTarget, String params) {
        resolveMethod(invokeTarget, params);
    }

    /**
     * 按调用目标反射执行业务方法
     *
     * @param job    任务定义（含调用目标与参数）
     * @return 方法返回值（无返回值时为 {@code null}）
     */
    public Object invoke(SysJob job) {
        Method method = resolveMethod(job.getInvokeTarget(), job.getParams());
        Object bean = getBean(job.getInvokeTarget());
        Object[] args = buildArgs(method, job.getParams());
        try {
            return method.invoke(bean, args);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new IllegalStateException("定时任务执行失败: " + job.getInvokeTarget(), cause);
        }
    }

    // ================ 内部方法 ================

    /**
     * 解析调用目标为 Method：beanName.method() → Bean + 形参数匹配的方法
     */
    private Method resolveMethod(String invokeTarget, String params) {
        if (StrUtil.isBlank(invokeTarget) || !invokeTarget.contains(".")
                || !invokeTarget.endsWith("()")) {
            throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                    "格式应为 beanName.method()，实际为「" + invokeTarget + "」");
        }
        Object bean = getBean(invokeTarget);
        String methodName = methodNameOf(invokeTarget);
        int argCount = countParams(params);
        for (Method method : bean.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                "未找到方法「" + methodName + "」（形参数 " + argCount + " 与任务参数不匹配）");
    }

    private Object getBean(String invokeTarget) {
        String beanName = StrUtil.subBefore(invokeTarget, ".", false);
        try {
            return applicationContext.getBean(beanName);
        } catch (NoSuchBeanDefinitionException e) {
            throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID, "Bean 不存在: " + beanName);
        }
    }

    private String methodNameOf(String invokeTarget) {
        return StrUtil.subBetween(invokeTarget, ".", "()");
    }

    /**
     * 参数键值对数量（空参数为 0），并顺带校验 JSON 对象格式
     */
    private int countParams(String params) {
        if (StrUtil.isBlank(params)) {
            return 0;
        }
        Map<String, Object> map = parseParams(params);
        return map.size();
    }

    private Map<String, Object> parseParams(String params) {
        try {
            return JacksonUtils.parseObject(params, new tools.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception e) {
            throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                    "任务参数必须为 JSON 对象（如 {\"key\":\"value\"}）");
        }
    }

    /**
     * 按方法形参构造实参（无参返回空数组）
     */
    private Object[] buildArgs(Method method, String params) {
        int argCount = method.getParameterCount();
        if (argCount == 0) {
            return new Object[0];
        }
        if (StrUtil.isBlank(params)) {
            throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                    "方法「" + method.getName() + "」需要参数，但任务未配置参数");
        }
        if (argCount == 1) {
            Class<?> type = method.getParameterTypes()[0];
            Object value = String.class.equals(type) ? params : parseParams(params);
            return new Object[]{convertValue(value, type)};
        }
        Map<String, Object> paramValues = parseParams(params);
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            String name = parameters[i].getName();
            if (!paramValues.containsKey(name)) {
                throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                        "任务参数缺少形参「" + name + "」对应的值");
            }
            args[i] = convertValue(paramValues.get(name), parameters[i].getType());
        }
        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        try {
            return JacksonUtils.getObjectMapper().convertValue(value, targetType);
        } catch (Exception e) {
            throw exception(SystemErrorCode.JOB_INVOKE_TARGET_INVALID,
                    "参数值无法转换为 " + targetType.getSimpleName() + ": " + value);
        }
    }
}
