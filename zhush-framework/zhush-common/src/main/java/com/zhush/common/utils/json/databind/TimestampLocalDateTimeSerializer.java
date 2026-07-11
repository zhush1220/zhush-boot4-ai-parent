package com.zhush.common.utils.json.databind;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 自定义 LocalDateTime 序列化器
 * <p>
 * 将 {@link LocalDateTime} 对象序列化为 Long 类型的时间戳（毫秒级）
 *
 * @author zhush
 * @date 2026/7/1 星期三 12:17
 */
public class TimestampLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext serializers) {
        if (value != null) {
            gen.writeNumber(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } else {
            gen.writeNull();
        }
    }
}
