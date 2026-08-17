package com.zhush.mq.core;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RocketMQUtils 消息发送封装单测（Mockito 模拟 DefaultMQProducer，不连 Broker）
 *
 * @author zhush
 */
@ExtendWith(MockitoExtension.class)
class RocketMQUtilsTest {

    @Mock
    private DefaultMQProducer producer;

    private RocketMQUtils utils;

    @BeforeEach
    void setUp() {
        utils = new RocketMQUtils(producer);
    }

    @Test
    void sendSyncWithStringPayloadAndEmptyTag() throws Exception {
        SendResult result = mock(SendResult.class);
        when(producer.send(any(Message.class))).thenReturn(result);

        assertThat(utils.sendSync("topicA", "hello")).isSameAs(result);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        Message message = captor.getValue();
        assertThat(message.getTopic()).isEqualTo("topicA");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("hello");
        // 空 tag/key 规范化为 null
        assertThat(message.getTags()).isNull();
        assertThat(message.getKeys()).isNull();
        assertThat(message.getDelayTimeLevel()).isZero();
    }

    @Test
    void sendSyncWithTagAndKeys() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(mock(SendResult.class));

        utils.sendSync("topicA", "tagA", "key-1", "hello");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        assertThat(captor.getValue().getTags()).isEqualTo("tagA");
        assertThat(captor.getValue().getKeys()).isEqualTo("key-1");
    }

    @Test
    void sendSyncWithObjectPayloadSerializedToJson() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(mock(SendResult.class));

        utils.sendSync("topicA", Map.of("id", 1, "name", "zhush"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        // Map.of 迭代顺序不固定，解析后断言内容
        Map<String, Object> parsed = com.zhush.common.utils.json.JacksonUtils.parseObject(
                new String(captor.getValue().getBody(), StandardCharsets.UTF_8),
                new TypeReference<Map<String, Object>>() {
                });
        assertThat(parsed).containsEntry("id", 1).containsEntry("name", "zhush");
    }

    @Test
    void sendSyncWithByteArrayPayloadKeepsRawBytes() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(mock(SendResult.class));

        byte[] raw = new byte[]{1, 2, 3};
        utils.sendSync("topicA", raw);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        assertThat(captor.getValue().getBody()).isEqualTo(raw);
    }

    @Test
    void sendDelaySetsDelayTimeLevel() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(mock(SendResult.class));

        utils.sendDelay("topicA", "late", 5);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        assertThat(captor.getValue().getDelayTimeLevel()).isEqualTo(5);
    }

    @Test
    void sendAsyncUsesCallback() throws Exception {
        SendCallback callback = mock(SendCallback.class);

        utils.sendAsync("topicA", "async", callback);

        verify(producer).send(any(Message.class), eq(callback));
    }

    @Test
    void sendOnewayDelegates() throws Exception {
        utils.sendOneway("topicA", "oneway");

        verify(producer).sendOneway(any(Message.class));
    }

    @Test
    void sendOrderlySelectsQueueDeterministicallyByHashKey() throws Exception {
        when(producer.send(any(Message.class), any(MessageQueueSelector.class), any(Object.class)))
                .thenReturn(mock(SendResult.class));

        utils.sendOrderly("topicA", "order", "order-key");

        ArgumentCaptor<MessageQueueSelector> selectorCaptor =
                ArgumentCaptor.forClass(MessageQueueSelector.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(producer).send(any(Message.class), selectorCaptor.capture(), argCaptor.capture());
        assertThat(argCaptor.getValue()).isEqualTo("order-key");

        // 同一 hashKey 多次选择结果一致（分区有序）
        List<MessageQueue> queues = List.of(
                new MessageQueue("brokerA", "topicA", 0),
                new MessageQueue("brokerA", "topicA", 1),
                new MessageQueue("brokerA", "topicA", 2));
        MessageQueue first = selectorCaptor.getValue().select(queues, mock(Message.class), "order-key");
        MessageQueue second = selectorCaptor.getValue().select(queues, mock(Message.class), "order-key");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void sendFailureWrappedAsIllegalStateException() throws Exception {
        when(producer.send(any(Message.class))).thenThrow(new RuntimeException("broker down"));

        assertThatThrownBy(() -> utils.sendSync("topicA", "hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RocketMQ 同步发送失败: topic=topicA");
    }
}
