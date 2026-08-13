package com.zhush.mq.config;

import com.zhush.mq.core.MqListenerContainer;
import com.zhush.mq.core.RocketMQUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 自动装配
 *
 * <p>装配内容：
 * <ul>
 *     <li>{@link DefaultMQProducer}：生产者（受 {@code zhush.mq.producer.enabled} 子开关控制），
 *     启动时完成 start，销毁时 shutdown</li>
 *     <li>{@link RocketMQUtils}：同步/异步/单向/延迟/顺序消息发送封装</li>
 *     <li>{@link MqListenerContainer}：扫描 {@code @ZhushMqListener} 自动创建消费者</li>
 * </ul>
 *
 * <p>启停控制：{@code zhush.mq.enabled}（默认 false，关闭时零装配零连接）。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(DefaultMQProducer.class)
@EnableConfigurationProperties(RocketMQProperties.class)
@ConditionalOnProperty(prefix = RocketMQProperties.PREFIX, name = "enabled", havingValue = "true")
public class RocketMQAutoConfiguration {

    /**
     * 生产者（initMethod/destroyMethod 托管生命周期）
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = RocketMQProperties.PREFIX + ".producer",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public DefaultMQProducer defaultMQProducer(RocketMQProperties properties) {
        RocketMQProperties.Producer producerConfig = properties.getProducer();
        DefaultMQProducer producer = new DefaultMQProducer(producerConfig.getGroup(), buildAclHook(properties));
        producer.setNamesrvAddr(properties.getNamesrvAddr());
        producer.setSendMsgTimeout(producerConfig.getSendTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimes());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        return producer;
    }

    /**
     * 消息发送封装（生产者关闭时不装配）
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMQUtils rocketMQUtils(ObjectProvider<DefaultMQProducer> producerProvider) {
        DefaultMQProducer producer = producerProvider.getIfAvailable();
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未装配（zhush.mq.producer.enabled=false 时不可发送消息）");
        }
        return new RocketMQUtils(producer);
    }

    /**
     * 消费者容器（自动发现 @ZhushMqListener 监听器）
     */
    @Bean
    @ConditionalOnMissingBean
    public MqListenerContainer mqListenerContainer(ApplicationContext applicationContext,
                                                   RocketMQProperties properties) {
        return new MqListenerContainer(applicationContext, properties);
    }

    private RPCHook buildAclHook(RocketMQProperties properties) {
        if (properties.getAccessKey() == null || properties.getAccessKey().isBlank()) {
            return null;
        }
        return new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey()));
    }
}
