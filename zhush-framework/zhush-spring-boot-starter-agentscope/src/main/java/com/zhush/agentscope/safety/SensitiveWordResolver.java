package com.zhush.agentscope.safety;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态敏感词解析器：缓存启用态敏感词（由 {@link SensitiveWordProvider} 加载），
 * 对工具名/入参内容做分级命中检测。
 *
 * <p>读取顺序：动态敏感词库（优先） → yaml {@code safety.sensitive-keywords}（回退，按审批级别处理）。
 *
 * @author zhush
 */
public class SensitiveWordResolver {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordResolver.class);

    private static final String MATCH_TOOL_NAME = "tool_name";
    private static final String MATCH_CONTENT = "content";

    private final List<SensitiveWordProvider> providers;
    private final AgentScopeProperties properties;

    /** 启用态敏感词缓存，volatile 整体替换保证读一致性 */
    private volatile List<SensitiveWordConfig> cache = List.of();

    public SensitiveWordResolver(List<SensitiveWordProvider> providers, AgentScopeProperties properties) {
        this.providers = providers;
        this.properties = properties;
        this.refresh();
    }

    /**
     * 重新从 Provider 加载启用态敏感词（变更/广播/定时触发）。
     */
    public synchronized void refresh() {
        List<SensitiveWordConfig> next = new ArrayList<>();
        for (SensitiveWordProvider provider : providers) {
            try {
                List<SensitiveWordConfig> words = provider.enabledWords();
                if (words != null) {
                    for (SensitiveWordConfig word : words) {
                        if (word != null && word.getWord() != null && !word.getWord().isBlank()) {
                            next.add(word);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("动态敏感词加载失败, provider={}", provider.getClass().getSimpleName(), e);
            }
        }
        this.cache = next;
        log.info("动态敏感词库刷新完成, 启用词数={}", next.size());
    }

    /**
     * 分级命中检测：工具名与入参内容分别匹配，命中词取最高级别。
     *
     * @param toolName  工具名
     * @param inputJson 工具入参（JSON 字符串，可为 null）
     * @return 命中结果（未命中返回 {@link SensitiveWordMatch#none()}）
     */
    public SensitiveWordMatch match(String toolName, String inputJson) {
        int level = SensitiveWordMatch.LEVEL_NONE;
        List<String> hitWords = new ArrayList<>();
        for (SensitiveWordConfig word : cache) {
            String matchType = word.getMatchType() == null ? MATCH_TOOL_NAME : word.getMatchType();
            boolean toolHit = !MATCH_CONTENT.equals(matchType)
                    && toolName != null && toolName.contains(word.getWord());
            boolean contentHit = !MATCH_TOOL_NAME.equals(matchType)
                    && inputJson != null && inputJson.contains(word.getWord());
            if (toolHit || contentHit) {
                hitWords.add(word.getWord());
                level = Math.max(level, word.getLevel());
            }
        }
        if (level > SensitiveWordMatch.LEVEL_NONE) {
            return SensitiveWordMatch.of(level, hitWords);
        }
        // 回退 yaml 敏感词（全部按审批级别处理，保持存量语义）
        List<String> yamlKeywords = properties.getSafety().getSensitiveKeywords();
        if (yamlKeywords != null && toolName != null) {
            List<String> yamlHits = new ArrayList<>();
            for (String keyword : yamlKeywords) {
                if (toolName.contains(keyword)) {
                    yamlHits.add(keyword);
                }
            }
            if (!yamlHits.isEmpty()) {
                return SensitiveWordMatch.of(SensitiveWordMatch.LEVEL_APPROVAL, yamlHits);
            }
        }
        return SensitiveWordMatch.none();
    }
}
