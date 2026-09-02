package com.zhush.agentscope.safety;

/**
 * 动态敏感词配置（由业务模块通过 {@link SensitiveWordProvider} 输出）。
 *
 * @author zhush
 */
public class SensitiveWordConfig {

    /** 敏感词 */
    private String word;

    /** 匹配对象：tool_name-工具名 content-入参/内容 both-两者 */
    private String matchType;

    /** 命中级别：1-提示 2-审批 3-拦截 */
    private int level;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
