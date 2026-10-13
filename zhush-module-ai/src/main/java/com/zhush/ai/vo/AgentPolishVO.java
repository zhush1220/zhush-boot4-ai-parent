package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 输入优化（润色）结果。
 *
 * @author zhush
 */
@Data
public class AgentPolishVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 优化后的文本（前端以预览形式展示，用户确认后采纳回填输入框） */
    private String optimized;
}
