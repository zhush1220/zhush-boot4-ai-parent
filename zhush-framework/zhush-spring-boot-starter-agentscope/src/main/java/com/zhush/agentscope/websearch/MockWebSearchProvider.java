package com.zhush.agentscope.websearch;

import java.util.ArrayList;
import java.util.List;

/**
 * 无网络桩供应商（{@code provider: mock}）：返回确定性假结果，用于 CI 与「先验证链路、后接供应商」。
 *
 * <p>不产生任何出网请求，也不消耗供应商额度，但<b>仍走完整的缓存/配额/格式化链路</b>，
 * 因此装配错误、开关不生效、工具未挂载这类问题能在本地暴露。
 *
 * @author zhush
 */
public class MockWebSearchProvider implements WebSearchProvider {

    public static final String CODE = "mock";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<WebSearchResult> search(String query, int maxResults) {
        int count = Math.max(1, Math.min(maxResults, 3));
        List<WebSearchResult> results = new java.util.ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            results.add(WebSearchResult.builder()
                    .title("[mock] " + query + " - 结果 " + i)
                    .url("https://example.com/mock/" + i)
                    .snippet("这是 mock 供应商返回的占位摘要，仅用于链路验证，不代表真实检索结果。")
                    .source("example.com")
                    .publishedAt("2026-01-01T00:00:00+08:00")
                    .build());
        }
        return results;
    }
}
