package com.zhush.common.utils.json.databind;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 自定义 LocalDateTime 反序列化器
 * <p>
 * 将 Long 类型的时间戳（毫秒级）反序列化为 {@link LocalDateTime} 对象
 *
 * @author zhush
 * @date 2026/7/1 星期三 12:17
 */
public class TimestampLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        long timestamp = p.getLongValue();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
