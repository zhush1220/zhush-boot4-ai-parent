package com.zhush.ai.provider;

import com.zhush.agentscope.safety.SensitiveWordConfig;
import com.zhush.agentscope.safety.SensitiveWordProvider;
import com.zhush.ai.entity.AiSensitiveWordEntity;
import com.zhush.ai.service.AiSensitiveWordService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态敏感词提供者（DB 实现）：向框架输出 {@code ai_sensitive_word} 启用态敏感词。
 *
 * @author zhush
 */
@Component
public class DbSensitiveWordProvider implements SensitiveWordProvider {

    private final AiSensitiveWordService wordService;

    public DbSensitiveWordProvider(AiSensitiveWordService wordService) {
        this.wordService = wordService;
    }

    @Override
    public List<SensitiveWordConfig> enabledWords() {
        List<AiSensitiveWordEntity> entities = wordService.enabledEntities();
        List<SensitiveWordConfig> result = new ArrayList<>(entities.size());
        for (AiSensitiveWordEntity entity : entities) {
            SensitiveWordConfig config = new SensitiveWordConfig();
            config.setWord(entity.getWord());
            config.setMatchType(entity.getMatchType());
            config.setLevel(entity.getLevel() == null ? 2 : entity.getLevel());
            result.add(config);
        }
        return result;
    }
}
