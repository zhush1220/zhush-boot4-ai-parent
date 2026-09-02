package com.zhush.agentscope.safety;

import java.util.List;

/**
 * 敏感词命中结果。
 *
 * @author zhush
 */
public class SensitiveWordMatch {

    /** 未命中（放行） */
    public static final int LEVEL_NONE = 0;
    /** 提示：放行执行并留痕 */
    public static final int LEVEL_NOTICE = 1;
    /** 审批：挂起走人工审批 */
    public static final int LEVEL_APPROVAL = 2;
    /** 拦截：直接拒绝执行 */
    public static final int LEVEL_BLOCK = 3;

    private final boolean hit;
    private final int level;
    private final List<String> words;

    private SensitiveWordMatch(boolean hit, int level, List<String> words) {
        this.hit = hit;
        this.level = level;
        this.words = words;
    }

    public static SensitiveWordMatch none() {
        return new SensitiveWordMatch(false, LEVEL_NONE, List.of());
    }

    public static SensitiveWordMatch of(int level, List<String> words) {
        return new SensitiveWordMatch(true, level, words);
    }

    public boolean isHit() {
        return hit;
    }

    public int getLevel() {
        return level;
    }

    public List<String> getWords() {
        return words;
    }
}
