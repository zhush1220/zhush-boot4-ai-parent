package com.zhush.common.utils.json;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JacksonUtils 序列化/反序列化单测
 *
 * @author zhush
 */
class JacksonUtilsTest {

    /** 测试 POJO（record 验证 Jackson 3 原生支持） */
    record Demo(Long id, String name, LocalDateTime time) {
    }

    @Test
    void localDateTimeSerializesAsTimestampAndRoundTrips() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 1, 10, 0, 0);
        long expectedMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Demo demo = new Demo(1L, "zhush", time);
        String json = JacksonUtils.toJsonString(demo);

        assertThat(json).contains(String.valueOf(expectedMillis));

        Demo restored = JacksonUtils.parseObject(json, Demo.class);
        assertThat(restored).isEqualTo(demo);
    }

    @Test
    void nullFieldsAreOmitted() {
        String json = JacksonUtils.toJsonString(new Demo(null, "zhush", null));
        assertThat(json).doesNotContain("null").contains("\"name\":\"zhush\"");
    }

    @Test
    void unknownPropertiesAreIgnored() {
        Demo demo = JacksonUtils.parseObject("{\"id\":1,\"name\":\"zhush\",\"unknown\":99}", Demo.class);
        assertThat(demo).isEqualTo(new Demo(1L, "zhush", null));
    }

    @Test
    void parseArray() {
        String json = "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]";
        List<Demo> list = JacksonUtils.parseArray(json, Demo.class);
        assertThat(list).hasSize(2).extracting(Demo::name).containsExactly("a", "b");
    }

    @Test
    void parseArrayEmptyReturnsEmptyList() {
        assertThat(JacksonUtils.parseArray("", Demo.class)).isEmpty();
    }

    @Test
    void parseObjectEmptyReturnsNull() {
        assertThat(JacksonUtils.parseObject("", Demo.class)).isNull();
    }

    @Test
    void parseObjectQuietlyReturnsNullOnMalformedJson() {
        assertThat(JacksonUtils.parseObjectQuietly("{not-json", new TypeReference<Map<String, Object>>() {
        })).isNull();
    }

    @Test
    void toJsonByteAndParseBack() {
        Demo demo = new Demo(2L, "byte", null);
        byte[] bytes = JacksonUtils.toJsonByte(demo);
        assertThat(JacksonUtils.parseObject(bytes, Demo.class)).isEqualTo(demo);
    }

    @Test
    void malformedJsonThrowsRuntimeException() {
        assertThatThrownBy(() -> JacksonUtils.parseObject("{not-json", Demo.class))
                .isInstanceOf(RuntimeException.class);
    }
}
