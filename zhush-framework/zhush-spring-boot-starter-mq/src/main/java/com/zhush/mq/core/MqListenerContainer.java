package com.zhush.mq.core;

import com.zhush.common.utils.json.JacksonUtils;
import com.zhush.mq.config.RocketMQProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting RPCHook;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ 消费者容器
 *
 * <p>启动时扫描所有标注 {@link ZhushMqListener} 的 Bean，
 * 为每个监听器创建独立的 {@link DefaultMQPushConsumer} 并订阅对应主题；
 * 应用关闭时统一 shutdown，保证消息不丢不重（未 ACK 消息将被重投）。
 *
 * @author zhush
 */
@Slf4j
public class MqListenerContainer implements SmartLifecycle, SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    private final RocketMQProperties properties;

    private final List<DefaultMQPushConsumer> consumers = new ArrayList<>();

    private volatile boolean running = false;

    public MqListenerContainer(ApplicationContext applicationContext, RocketMQProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    /**
     * 全部单例就绪后注册监听器（晚于 Bean 创建，早于 start）
     */
    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ZhushMqListener.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            if (!(bean instanceof ZhushMessageListener<?> listener)) {
                throw new IllegalStateException("@ZhushMqListener 只能标注在 ZhushMessageListener 实现类上: "
                        + bean.getClass().getName());
            }
            register(entry.getKey(), bean, listener);
        }
        log.info("RocketMQ 监听器注册完成，共 {} 个", consumers.size());
    }

    private void register(String beanName, Object bean, ZhushMessageListener<?> listener) {
        // AnnotationUtils 兼容 CGLIB 代理类
        ZhushMqListener annotation = AnnotationUtils.findAnnotation(bean.getClass(), ZhushMqListener.class);
        if (annotation == null) {
            throw new IllegalStateException("无法解析 @ZhushMqListener 注解: " + beanName);
        }
        RocketMQProperties.Consumer consumerConfig = properties.getConsumer();
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                annotation.consumerGroup(), buildAclHook(), new AllocateMessageQueueAveragely());
        consumer.setNamesrvAddr(properties.getNamesrvAddr());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setConsumeThreadMin(annotation.consumeThreadMin() > 0
                ? annotation.consumeThreadMin() : consumerConfig.getConsumeThreadMin());
        consumer.setConsumeThreadMax(annotation.consumeThreadMax() > 0
                ? annotation.consumeThreadMax() : consumerConfig.getConsumeThreadMax());
        consumer.setMaxReconsumeTimes(consumerConfig.getMaxReconsumeTimes());
        try {
            consumer.subscribe(annotation.topic(), annotation.selectorExpression());
            Class<?> messageType = resolveMessageType(bean.getClass());
            consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                for (MessageExt message : messages) {
                    try {
                        invokeListener(listener, messageType, message);
                    } catch (Exception e) {
                        log.error("RocketMQ 消息消费失败: topic={}, msgId={}, 第 {} 次重试",
                                message.getTopic(), message.getMsgId(), message.getReconsumeTimes(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumers.add(consumer);
            log.info("注册 RocketMQ 监听器: {} -> topic={}, group={}, tags={}", beanName,
                    annotation.topic(), annotation.consumerGroup(), annotation.selectorExpression());
        } catch (MQClientException e) {
            throw new IllegalStateException("RocketMQ 监听器订阅失败: " + beanName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeListener(ZhushMessageListener<?> listener, Class<?> messageType,
                                MessageExt message) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Object payload;
        if (String.class.equals(messageType) || Object.class.equals(messageType)) {
            payload = body;
        } else {
            payload = JacksonUtils.parseObject(body, messageType);
        }
        MqMessage meta = new MqMessage(message.getTags(), message.getKeys(),
                message.getReconsumeTimes(), message.getMsgId(), message.getProperties(), message);
        ((ZhushMessageListener<Object>) listener).onMessage(payload, meta);
    }

    /**
     * 解析 ZhushMessageListener 的泛型实参（消息体类型）
     */
    private Class<?> resolveMessageType(Class<?> listenerClass) {
        ResolvableType type = ResolvableType.forClass(listenerClass).as(ZhushMessageListener.class);
        ResolvableType generic = type.getGeneric(0);
        Class<?> resolved = generic.resolve();
        return resolved == null ? Object.class : resolved;
    }

    private RPCHook buildAclHook() {
        if (properties.getAccessKey() == null || properties.getAccessKey().isBlank()) {
            return null;
        }
        return new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey()));
    }

    @Override
    public void start() {
        for (DefaultMQPushConsumer consumer : consumers) {
            try {
                consumer.start();
            } catch (MQClientException e) {
                throw new IllegalStateException("RocketMQ 消费者启动失败: group="
                        + consumer.getConsumerGroup(), e);
            }
        }
        running = true;
        log.info("RocketMQ 消费者已全部启动，共 {} 个", consumers.size());
    }

    @Override
    public void stop() {
        consumers.forEach(DefaultMQPushConsumer::shutdown);
        running = false;
        log.info("RocketMQ 消费者已全部关闭");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
