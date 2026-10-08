package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Jev 决策记录实体（ai_jev_decision_log）——判断模型观测/调优数据面。
 *
 * <p>口径（方案 14.4）：非降级记录按 {@code zhush.agentscope.jev.log-sample-rate} 采样，
 * degraded/sticky 记录全量保留；本表只写不读（后台观测直查 SQL），异步落库失败静默丢弃，
 * 不得影响决策主链路。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_jev_decision_log")
public class AiJevDecisionLogEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 场景：意图路由 */
    public static final int SCENE_INTENT = 1;
    /** 场景：模型路由（P1 预留） */
    public static final int SCENE_MODEL_ROUTER = 2;
    /** 场景：审批预判（P2 预留） */
    public static final int SCENE_AUTO_MODE = 3;

    /** 决策场景：1意图路由 2模型路由 3审批预判 */
    private Integer scene;

    /** 业务会话 ID（新建会话时为空串） */
    private String sessionId;

    /** 智能体名 */
    private String agentName;

    /** 归属用户 ID */
    private Long userId;

    /** 用户类型：admin/member */
    private String userType;

    /** 问题类型：noul/choice/score */
    private String questionType;

    /** 决策结果（choice=选项 code，noul=概率值） */
    private String answer;

    /** 置信度（0~1；降级记录为 null） */
    private BigDecimal confidence;

    /** 是否降级：0正常 1=Jev 失败/不可用回落 */
    private Integer degraded;

    /** 是否粘滞复用：0否 1=命中上轮分类未请求 Jev */
    private Integer sticky;

    /** 决策耗时（毫秒，含超时等待） */
    private Integer costMs;

    /** 降级原因（degraded=1 时记录） */
    private String errorMsg;
}
