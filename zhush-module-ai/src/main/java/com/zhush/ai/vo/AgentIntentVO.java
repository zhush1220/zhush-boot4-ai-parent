package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 意图分类结果（B1）。
 *
 * <p>契约（方案 14.2）：任何异常路径都返回 200 + {@code degraded=true} 回落 chat，
 * 前端拿不到高置信结果时按「auto=不注入」直接发送，分类失败不阻断对话主链路。
 *
 * @author zhush
 */
@Data
public class AgentIntentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类结果：chat / copywriting / docx / pdf / parse / image / video */
    private String taskType;

    /** 任务类型展示名（AiTaskType.desc，前端可直接提示） */
    private String taskTypeName;

    /** 置信度（0~1；降级时为 null） */
    private BigDecimal confidence;

    /** 是否降级：Jev 不可用/失败/低置信，已回落 chat */
    private boolean degraded;

    /** 是否粘滞复用：命中上轮分类未请求 Jev（观测口径，前端可忽略） */
    private boolean sticky;
}
