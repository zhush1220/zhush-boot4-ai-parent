package com.zhush.agentscope.tool;

import com.zhush.agentscope.context.AgentContext;
import io.agentscope.core.agent.RuntimeContext;
import tools.jackson.databind.ObjectMapper;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 业务工具 → AgentScope {@link io.agentscope.core.tool.AgentTool} 桥接器。
 *
 * <p>业务侧使用自定义 {@link AgentTool} 注解标注方法，AgentScope 无法识别该注解，
 * 因此由本类将 {@link RegisteredTool} 包装成 AgentScope 原生工具：反射执行目标方法，
 * 输出按文本 {@link ToolResultBlock} 返回。可通过 {@code gate} 注入人工审批等前置钩子。
 *
 * @author zhush
 */
public class ReflectiveAgentTool implements io.agentscope.core.tool.AgentTool {

    private final RegisteredTool registeredTool;
    private final ObjectMapper objectMapper;
    private final Function<ToolCallParam, Optional<ToolResultBlock>> gate;

    public ReflectiveAgentTool(RegisteredTool registeredTool, ObjectMapper objectMapper,
                               Function<ToolCallParam, Optional<ToolResultBlock>> gate) {
        this.registeredTool = registeredTool;
        this.objectMapper = objectMapper;
        this.gate = gate;
    }

    @Override
    public String getName() {
        return registeredTool.getName();
    }

    @Override
    public String getDescription() {
        return registeredTool.getDescription();
    }

    @Override
    public Map<String, Object> getParameters() {
        return paramsSchema(registeredTool.getMethod());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        if (gate != null) {
            Optional<ToolResultBlock> gated = gate.apply(param);
            if (gated.isPresent()) {
                return Mono.just(gated.get());
            }
        }
        try {
            Object result = invoke(param);
            String text = result == null ? "null" : result.toString();
            return Mono.just(ToolResultBlock.text(text));
        } catch (Throwable t) {
            return Mono.just(ToolResultBlock.error(t.getMessage()));
        }
    }

    /**
     * 反射调用目标方法，入参按名匹配（优先 {@link ToolParam#name()}，缺省按形参位置 pN）。
     *
     * <p>{@link AgentContext} 型形参不来自 LLM 入参，而由 {@link RuntimeContext} 属性袋注入，
     * 使工具能拿到可信归属（配额/审计/产物隔离）且不暴露给模型。
     */
    private Object invoke(ToolCallParam param) throws Exception {
        Method method = registeredTool.getMethod();
        Object bean = registeredTool.getBean();
        Parameter[] params = method.getParameters();
        Map<String, Object> input = param == null ? null : param.getInput();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            if (AgentContext.class.equals(p.getType())) {
                args[i] = resolveAgentContext(param);
                continue;
            }
            String key = paramKey(p, i);
            Object value = input == null ? null : input.get(key);
            if (value == null) {
                value = input == null ? null : input.get("p" + i);
            }
            args[i] = convert(value, p.getType());
        }
        return method.invoke(bean, args);
    }

    /**
     * 从 RuntimeContext 取业务上下文；无上下文（如本地直调、旧会话）时回退为匿名管理端身份，
     * 保证工具不因缺少归属而 NPE，同时日志级错误码可由调用方自行判定。
     */
    private AgentContext resolveAgentContext(ToolCallParam param) {
        RuntimeContext rc = param == null ? null : param.getRuntimeContext();
        if (rc != null) {
            AgentContext ctx = rc.get(AgentContext.class);
            if (ctx != null) {
                return ctx;
            }
        }
        return AgentContext.builder().userId(0L).userType("admin").build();
    }

    private Object convert(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType.isPrimitive() || targetType == Number.class || targetType == Boolean.class
                || targetType.isEnum() || targetType == java.math.BigDecimal.class
                || targetType == java.util.Date.class || targetType == java.time.LocalDate.class
                || targetType == java.time.LocalDateTime.class) {
            return objectMapper.convertValue(value, targetType);
        }
        // POJO / 集合：按 JSON 反序列化
        return objectMapper.convertValue(value, targetType);
    }

    private Map<String, Object> paramsSchema(Method method) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        if (method != null) {
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                Parameter p = params[i];
                // 框架注入型形参不告知 LLM，否则模型会尝试自行填写用户身份
                if (AgentContext.class.equals(p.getType())) {
                    continue;
                }
                String key = paramKey(p, i);
                Map<String, Object> field = new HashMap<>();
                field.put("type", jsonType(p.getType()));
                ToolParam tp = p.getAnnotation(ToolParam.class);
                if (tp != null && !tp.description().isEmpty()) {
                    field.put("description", tp.description());
                }
                properties.put(key, field);
                required.add(key);
            }
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private String paramKey(Parameter p, int index) {
        ToolParam tp = p.getAnnotation(ToolParam.class);
        if (tp != null && !tp.name().isEmpty()) {
            return tp.name();
        }
        return p.isNamePresent() ? p.getName() : "p" + index;
    }

    private String jsonType(Class<?> type) {
        if (type == String.class || type == Character.class) {
            return "string";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        }
        if (type == Integer.class || type == Long.class || type == Double.class
                || type == Float.class || type == Short.class || type == Byte.class
                || type == Number.class || type == BigDecimal.class
                || type == int.class || type == long.class || type == double.class) {
            return "number";
        }
        if (type == List.class) {
            return "array";
        }
        return "object";
    }
}
