package com.zhush.mq.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.Map;

/**
 * 消息元数据（tag、key、重试次数、用户属性、原生消息引用）
 *
 * @author zhush
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage {

    /**
     * 消息 Tag
     */
    private String tag;

    /**
     * 消息业务 Key
     */
    private String keys;

    /**
     * 已重试消费次数
     */
    private int reconsumeTimes;

    /**
     * 消息 ID
     */
    private String msgId;

    /**
     * 用户自定义属性
     */
    private Map<String, String> properties;

    /**
     * 原生消息（需要更多元数据时使用）
     */
    private transient MessageExt raw;
}
