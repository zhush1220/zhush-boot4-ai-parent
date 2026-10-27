package com.zhush.agentscope.websearch;

import java.io.IOException;
import java.util.List;

/**
 * 通用搜索供应商适配层（方案 5.1）：把「搜索」收敛成一个与供应商无关的接口，
 * 使 {@code provider} 可在配置里切换（生产 IQS、开发 Tavily、CI mock）而不动工具与业务代码。
 *
 * <p>失败契约：实现类以异常表达失败（IO/超时/响应结构不符），由 {@link WebSearchService} 统一
 * fail-open 降级为可读文本——联网失败绝不能打断对话主链路。
 *
 * @author zhush
 */
public interface WebSearchProvider {

    /**
     * 供应商标识（与 {@code zhush.agentscope.web-search.provider} 比较，忽略大小写）。
     */
    String code();

    /**
     * 执行一次开放域搜索。
     *
     * @param query      查询词
     * @param maxResults 期望返回条数（调用方已按上限夹紧）
     * @return 结果列表（空列表表示无命中，不为 null）
     * @throws IOException          网络失败、超时、非 2xx
     * @throws InterruptedException 请求线程被中断
     */
    List<WebSearchResult> search(String query, int maxResults) throws IOException, InterruptedException;
}
