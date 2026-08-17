package com.zhush.mq.config;

import com.zhush.mq.core.MqListenerContainer;
import com.zhush.mq.core.RocketMQUtils;
import com.zhush.test.StarterTestSupport;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RocketMQ starter 装配冒烟测试（开关开/关，生产者用 Mock 隔离真实连接）
 *
 * @author zhush
 */
class RocketMQAutoConfigurationTest extends StarterTestSupport {

    @Test
    void disabledByDefault() {
        runner(RocketMQAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DefaultMQProducer.class);
                    assertThat(context).doesNotHaveBean(RocketMQUtils.class);
                    assertThat(context).doesNotHaveBean(MqListenerContainer.class);
                });
    }

    @Test
    void disabledExplicitly() {
        runner(RocketMQAutoConfiguration.class)
                .withPropertyValues("zhush.mq.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RocketMQUtils.class));
    }

    @Test
    void enabledRegistersUtilsAndContainerWithMockProducer() {
        runner(RocketMQAutoConfiguration.class)
                .withPropertyValues("zhush.mq.enabled=true")
                .withBean(DefaultMQProducer.class, () -> mock(DefaultMQProducer.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(RocketMQUtils.class);
                    assertThat(context).hasSingleBean(MqListenerContainer.class);
                });
    }

    @Test
    void enabledButProducerDisabledFailsStartup() {
        runner(RocketMQAutoConfiguration.class)
                .withPropertyValues("zhush.mq.enabled=true", "zhush.mq.producer.enabled=false")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseInstanceOf(IllegalStateException.class));
    }
}
