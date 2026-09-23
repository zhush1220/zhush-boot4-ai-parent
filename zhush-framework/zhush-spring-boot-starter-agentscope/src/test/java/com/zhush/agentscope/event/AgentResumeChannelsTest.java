package com.zhush.agentscope.event;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link AgentResumeChannels} 续跑共享通道测试（S3）：
 * 覆盖「订阅后推送按序到达」「complete 终止流」「未知会话推送/回收为无操作不抛异常」。
 *
 * <p>用 Reactor 原生 {@code subscribe} + {@link CountDownLatch} 驱动，避免依赖 reactor-test。
 */
class AgentResumeChannelsTest {

    private AgentTextDeltaEvent event(String delta) {
        return AgentTextDeltaEvent.builder()
                .eventType(AgentEventType.TEXT_DELTA)
                .sessionId("s1")
                .delta(delta)
                .build();
    }

    @Test
    void emittedEventsReachSubscriberInOrder() throws Exception {
        AgentResumeChannels channels = new AgentResumeChannels();
        AgentTextDeltaEvent a = event("a");
        AgentTextDeltaEvent b = event("b");

        List<AgentEvent> received = new CopyOnWriteArrayList<>();
        CountDownLatch two = new CountDownLatch(2);
        Disposable subscription = channels.subscribe("s1").subscribe(e -> {
            received.add(e);
            two.countDown();
        });

        channels.emit("s1", a);
        channels.emit("s1", b);

        assertThat(two.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(received).containsExactly(a, b);
        subscription.dispose();
    }

    @Test
    void completeTerminatesActiveSubscription() throws Exception {
        AgentResumeChannels channels = new AgentResumeChannels();
        AtomicBoolean completed = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);
        Disposable subscription = channels.subscribe("s2").subscribe(e -> {
        }, err -> done.countDown(), () -> {
            completed.set(true);
            done.countDown();
        });

        channels.complete("s2");

        assertThat(done.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(completed).isTrue();
        subscription.dispose();
    }

    @Test
    void emitAndCompleteOnUnknownSessionAreNoOps() {
        AgentResumeChannels channels = new AgentResumeChannels();
        // 未建通道时推送/回收不得抛异常（续跑结果另经落库兜底）
        assertThatCode(() -> {
            channels.emit("ghost", event("x"));
            channels.complete("ghost");
        }).doesNotThrowAnyException();
    }

    @Test
    void nullArgumentsAreSafelyIgnored() {
        AgentResumeChannels channels = new AgentResumeChannels();
        assertThatCode(() -> {
            channels.emit(null, event("x"));
            channels.emit("s3", null);
            channels.complete(null);
        }).doesNotThrowAnyException();
    }
}
