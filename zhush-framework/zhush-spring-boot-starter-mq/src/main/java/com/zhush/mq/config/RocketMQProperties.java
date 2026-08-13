package com.zhush.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 配置属性
 *
 * <p>连接配置走本项目自有前缀 {@code zhush.mq.*}（而非 rocketmq-spring 前缀），
 * 与 starter 启停开关统一。
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = RocketMQProperties.PREFIX)
public class RocketMQProperties {

    public static final String PREFIX = "zhush.mq";

    /**
     * 是否启用 RocketMQ（总开关，默认关闭；关闭时不创建生产者/消费者，零装配）
     */
    private Boolean enabled = false;

    /**
     * NameServer 地址（多个用分号分隔，如 127.0.0.1:9876;127.0.0.2:9876）
     */
    private String namesrvAddr = "127.0.0.1:9876";

    /**
     * ACL 鉴权 accessKey（broker 未开启 ACL 时留空）
     */
    private String accessKey;

    /**
     * ACL 鉴权 secretKey
     */
    private String secretKey;

    /**
     * 生产者配置
     */
    private final Producer producer = new Producer();

    /**
     * 消费者默认配置（可被 @ZhushMqListener 注解属性覆盖）
     */
    private final Consumer consumer = new Consumer();

    @Data
    public static class Producer {

        /**
         * 是否启用生产者（默认随总开关启用）
         */
        private Boolean enabled = true;

        /**
         * 生产者分组
         */
        private String group = "zhush_producer_group";

        /**
         * 发送超时时间（毫秒）
         */
        private Integer sendTimeout = 3000;

        /**
         * 同步发送失败重试次数
         */
        private Integer retryTimes = 2;

        /**
         * 消息体最大长度（字节，默认 4MB）
         */
        private Integer maxMessageSize = 1024 * 1024 * 4;
    }

    @Data
    public static class Consumer {

        /**
         * 消费线程数下限
         */
        private Integer consumeThreadMin = 4;

        /**
         * 消费线程数上限
         */
        private Integer consumeThreadMax = 16;

        /**
         * 消费失败重试次数上限
         */
        private Integer maxReconsumeTimes = 3;
    }
}
