package com.zhush.mq.core;

/**
 * RocketMQ 消息监听器接口
 *
 * <p>泛型为消息体类型：{@code String} 时直接传递原文；
 * 其他类型由框架按 JSON 自动反序列化（发送端使用 {@code RocketMQUtils}
 * 发送对象时会 JSON 序列化，双端对齐）。
 *
 * <p>处理失败（抛出异常）时框架自动返回 RECONSUME_LATER 触发重试，
 * 超过最大重试次数后进入死信队列。
 *
 * @param <T> 消息体类型
 * @author zhush
 */
public interface ZhushMessageListener<T> {

    /**
     * 消息处理逻辑
     *
     * @param message 反序列化后的消息体
     * @param meta    消息元数据（tag/keys/重试次数等）
     * @throws Exception 抛出异常将触发消息重试
     */
    void onMessage(T message, MqMessage meta) throws Exception;
}
