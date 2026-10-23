package com.zhush.agentscope.websearch;

import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 供应商适配器共用的 HTTP 打桩：固定返回一段 JSON，并在被测方法调用后取回真正发出的请求。
 *
 * <p>报文里的字段（计费用的 {@code contents} 开关、条数上限）是外部契约：写错不会编译失败、
 * 只会静默多花钱或拿不到结果，所以请求体也要读出来断言。
 */
final class WebSearchProviderTestSupport {

    private WebSearchProviderTestSupport() {
    }

    /** 带类型的任意 matcher：让 {@code send} 的泛型推断落在 String 上，避免 stub 链退化为原始类型 */
    @SuppressWarnings("unchecked")
    static HttpResponse.BodyHandler<String> anyStringBody() {
        return any(HttpResponse.BodyHandler.class);
    }

    /** 预约一次响应：状态码 + 响应体 */
    @SuppressWarnings("unchecked")
    static void respond(HttpClient client, int status, String responseBody) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(responseBody);
        when(client.send(any(HttpRequest.class), anyStringBody())).thenReturn(response);
    }

    /** 被测方法调用之后取回实际发出的请求 */
    static HttpRequest captured(HttpClient client) throws IOException, InterruptedException {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), anyStringBody());
        return captor.getValue();
    }

    /** 断言请求要点：POST、端点、Bearer 鉴权与 JSON 头 */
    static void assertJsonPost(HttpRequest request, String expectedUrl, String apiKey) {
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri()).isEqualTo(URI.create(expectedUrl));
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer " + apiKey);
        assertThat(request.headers().firstValue("Content-Type")).hasValue("application/json");
    }

    /** 读出请求体 JSON（publisher 可能分多块，先聚字节再整体解码，避免多字节字符被切断） */
    static JsonNode body(HttpRequest request) throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Throwable[] failure = new Throwable[1];
        CountDownLatch done = new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                out.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                failure[0] = throwable;
                done.countDown();
            }

            @Override
            public void onComplete() {
                done.countDown();
            }
        });
        assertThat(done.await(2, TimeUnit.SECONDS)).as("请求体读取超时").isTrue();
        assertThat(failure[0]).isNull();
        return new ObjectMapper().readTree(out.toString(StandardCharsets.UTF_8));
    }
}
