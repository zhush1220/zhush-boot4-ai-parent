package com.zhush.common.utils.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import com.zhush.common.utils.json.databind.TimestampLocalDateTimeSerializer;
import com.zhush.common.utils.json.databind.TimestampLocalDateTimeDeserializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @ClassName JacksonUtils
 * @Description JSON工具类
 * @Author zhush
 * @Date 2026/7/1 星期三 10:08
 *
 **/
@Slf4j
public class JacksonUtils {

    @Getter
    private static ObjectMapper objectMapper;

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    static {
        SimpleModule module = new SimpleModule();
        // 注册 LocalDateTime ↔ Long 时间戳 序列化/反序列化器
        module.addSerializer(LocalDateTime.class, new TimestampLocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new TimestampLocalDateTimeDeserializer());

        objectMapper = JsonMapper.builder()
                // 忽略空 Bean 序列化异常，避免在序列化时出现 "No serializer found for type" 的错误
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 忽略在反序列化时出现的未知属性，避免出现 "Unrecognized field" 的错误
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 忽略 null 值，避免在序列化时出现 "null" 字符串
                .changeDefaultPropertyInclusion(
                        incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                )
                .addModule(module)
                .build();
    }

    /**
     * 初始化 objectMapper 属性
     * <p>
     * 通过这样的方式，使用 Spring 创建的 ObjectMapper Bean
     *
     * @param objectMapper ObjectMapper 对象
     */
    public static void init(ObjectMapper objectMapper) {
        JacksonUtils.objectMapper = objectMapper;
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param object 对象
     * @return JSON 字符串
     */
    public static String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            log.error("json serialize err, json: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转换为 JSON 字节数组
     *
     * @param object 对象
     * @return JSON 字节数组
     */
    public static byte[] toJsonByte(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JacksonException e) {
            log.error("json serialize err,json:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转换为 JSON 字符串，并进行格式化
     *
     * @param object 对象
     * @return JSON 字符串
     */
    public static String toJsonPrettyString(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JacksonException e) {
            log.error("json serialize err,json:{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param text   JSON 字符串
     * @param clazz  对象类
     * @return 对象
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param text   JSON 字符串
     * @param path   JSON 路径
     * @param clazz  对象类
     * @return 对象
     */
    public static <T> T parseObject(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), clazz);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param text   JSON 字符串
     * @param type   对象类型
     * @return 对象
     */
    public static <T> T parseObject(String text, Type type) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     * @param text   JSON 字符串
     * @param clazz  对象类
     * @return 对象
     */
    public static <T> T parseObject2(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        return JSONUtil.toBean(text, clazz);
    }

    /**
     * 将 JSON 字节数组转换为对象
     *
     * @param bytes  JSON 字节数组
     * @param clazz  对象类
     * @return 对象
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", bytes, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param text          JSON 字符串
     * @param typeReference 对象类型
     * @return 对象
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象，如果转换失败则返回 null
     *
     * @param text          JSON 字符串
     * @param typeReference 对象类型引用
     * @return 指定类型的对象
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (JacksonException e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为对象列表
     *
     * @param text   JSON 字符串
     * @param clazz  对象类
     * @return 对象列表
     */
    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为对象列表
     *
     * @param text   JSON 字符串
     * @param path   JSON 路径
     * @param clazz  对象类
     * @return 对象列表
     */
    public static <T> List<T> parseArray(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串转换为 JsonNode
     *
     * @param text JSON 字符串
     * @return JsonNode
     */
    public static JsonNode parseTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组转换为 JsonNode
     *
     * @param text JSON 字节数组
     * @return JsonNode
     */
    public static JsonNode parseTree(byte[] text) {
        try {
            return objectMapper.readTree(text);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断字符串是否是 JSON 格式
     *
     * @param text JSON 字符串
     * @return 是否是 JSON 格式
     */
    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 判断字符串是否是 JSON 对象格式
     *
     * @param str JSON 字符串
     * @return 是否是 JSON 对象格式
     */
    public static boolean isJsonObject(String str) {
        return JSONUtil.isTypeJSONObject(str);
    }
}
