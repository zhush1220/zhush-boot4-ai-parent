package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI 按人按日用量实体（ai_usage_record）
 *
 * <p>聚合键为「用户 + 用户类型 + 日期 + 任务类型」，既作配额判定的计数来源，也让成本可归人
 * （{@code agent_token_usage} 只到租户/智能体维度，无法回答「某人今天用了多少」）。
 *
 * <p>与 {@code agent_token_usage} 并存不冲突：前者是人维度、后者是智能体维度，
 * 由同一事件分别累加，不双写同一列。聚合行只增不删（{@code is_delete} 参与唯一键）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_usage_record")
public class AiUsageRecordEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期 */
    private LocalDate usageDate;

    /** 用户ID */
    private Long userId;

    /** 用户类型：admin-管理端 member-会员端 */
    private String userType;

    /** 任务类型：chat/copywriting/docx/pdf/parse/image/video */
    private String taskType;

    /** 请求次数（对话/导出/解析等文本类能力计数） */
    private Integer requestCount;

    /** 图像生成成功数 */
    private Integer imageCount;

    /** 视频生成成功数 */
    private Integer videoCount;

    /** 输入 Token 累计 */
    private Long inputTokens;

    /** 输出 Token 累计 */
    private Long outputTokens;

    /** 总 Token 累计 */
    private Long totalTokens;

    /** 预估成本（按模型单价折算） */
    private BigDecimal cost;
}
