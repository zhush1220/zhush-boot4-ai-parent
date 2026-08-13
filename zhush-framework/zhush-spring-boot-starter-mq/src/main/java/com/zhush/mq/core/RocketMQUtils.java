package com.zhush.mq.core;

import com.zhush.common.utils.json.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 消息发送封装
 *
 * <p>支持同步/异步/单向、延迟消息、顺序消息；对象消息自动 JSON 序列化，
 * 与 {@link ZhushMessageListener} 的 JSON 反序列化对齐。
 *
 * <p>延迟等级（delayLevel 1-18）对应：
 * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
 *
 * @author zhush
 */
@Slf4j
public class RocketMQUtils {

    private final DefaultMQProducer producer;

    public RocketMQUtils(DefaultMQProducer producer) {
        this.producer = producer;
    }

    /**
     * 同步发送（等待 broker 确认，返回发送结果）
     */
    public SendResult sendSync(String topic, Object payload) {
        return sendSync(topic, "", "", payload);
    }

    /**
     * 同步发送（带 Tag 与业务 Key）
     */
    public SendResult sendSync(String topic, String tag, String keys, Object payload) {
        try {
            return producer.send(buildMessage(topic, tag, keys, payload, 0));
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 同步发送失败: topic=" + topic, e);
        }
    }

    /**
     * 异步发送（结果经回调返回）
     */
    public void sendAsync(String topic, Object payload, SendCallback callback) {
        sendAsync(topic, "", "", payload, callback);
    }

    /**
     * 异步发送（带 Tag 与业务 Key）
     */
    public void sendAsync(String topic, String tag, String keys, Object payload, SendCallback callback) {
        try {
            producer.send(buildMessage(topic, tag, keys, payload, 0), callback);
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 异步发送失败: topic=" + topic, e);
        }
    }

    /**
     * 单向发送（不等待确认，适用于日志类可丢失消息）
     */
    public void sendOneway(String topic, Object payload) {
        try {
            producer.sendOneway(buildMessage(topic, "", "", payload, 0));
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 单向发送失败: topic=" + topic, e);
        }
    }

    /**
     * 延迟消息（delayLevel 1-18，见类注释对照表）
     */
    public SendResult sendDelay(String topic, Object payload, int delayLevel) {
        try {
            return producer.send(buildMessage(topic, "", "", payload, delayLevel));
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 延迟发送失败: topic=" + topic, e);
        }
    }

    /**
     * 顺序消息（同 hashKey 的消息进入同一队列，保证分区有序）
     */
    public SendResult sendOrderly(String topic, Object payload, String hashKey) {
        try {
            return producer.send(buildMessage(topic, "", "", payload, 0),
                    (queues, msg, arg) -> queues.get(Math.abs(arg.hashCode()) % queues.size()), hashKey);
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 顺序发送失败: topic=" + topic, e);
        }
    }

    /**
     * 获取底层生产者（需要未封装能力时使用）
     */
    public DefaultMQProducer getProducer() {
        return producer;
    }

    private Message buildMessage(String topic, String tag, String keys, Object payload, int delayLevel) {
        byte[] body;
        if (payload instanceof byte[] bytes) {
            body = bytes;
        } else if (payload instanceof String str) {
            body = str.getBytes(StandardCharsets.UTF_8);
        } else {
            body = JacksonUtils.toJsonString(payload).getBytes(StandardCharsets.UTF_8);
        }
        Message message = new Message(topic, emptyToNull(tag), emptyToNull(keys), body);
        if (delayLevel > 0) {
            message.setDelayTimeLevel(delayLevel);
        }
        return message;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
