package com.zhush.mq.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RocketMQ 消息监听器注解
 *
 * <p>标注在实现 {@link ZhushMessageListener} 的 Spring Bean 上，
 * starter 启动时自动创建对应消费者并订阅。示例：
 *
 * <pre>{@code
 * @Component
 * @ZhushMqListener(topic = "order_topic", consumerGroup = "order_group")
 * public class OrderListener implements ZhushMessageListener<OrderMessage> {
 *     @Override
 *     public void onMessage(OrderMessage message, MqMessage raw) {
 *         // 业务处理；抛出异常将触发重试
 *     }
 * }
 * }</pre>
 *
 * @author zhush
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZhushMqListener {

    /**
     * 订阅主题
     */
    String topic();

    /**
     * 消费者分组（不同分组独立消费；同分组集群消费）
     */
    String consumerGroup();

    /**
     * Tag 过滤表达式（默认全部，多个用 || 分隔，如 "TagA || TagB"）
     */
    String selectorExpression() default "*";

    /**
     * 消费线程数下限（<=0 使用 zhush.mq.consumer 默认值）
     */
    int consumeThreadMin() default 0;

    /**
     * 消费线程数上限（<=0 使用 zhush.mq.consumer 默认值）
     */
    int consumeThreadMax() default 0;
}
