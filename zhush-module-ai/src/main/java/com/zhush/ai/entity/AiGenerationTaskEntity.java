package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 生成任务实体（ai_generation_task）——图像/视频异步生成状态机。
 *
 * <p>生命周期：工具同步「提交」即落库（{@code status=0 排队}）→ 转存线程池轮询供应商 →
 * 命中产物后下载并转存 {@code infra_file}（{@code status=2 成功}，写 {@code file_id/result_url}）→
 * 失败/超时收敛为 {@code 3/4}。供应商产物 URL 约 24h 过期，故必须转存，前端一律用转存后的稳定外链。
 *
 * <p>{@code task_id} 为对外 UUID（前端轮询凭据），{@code remote_task_id} 为供应商任务号；
 * {@code message_id=0} 表示未回填对话消息（产物经轮询接口呈现，历史回显尽力回填至最近助手消息）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_generation_task")
public class AiGenerationTaskEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态：排队中 */
    public static final int STATUS_QUEUED = 0;
    /** 状态：运行中 */
    public static final int STATUS_RUNNING = 1;
    /** 状态：成功 */
    public static final int STATUS_SUCCEEDED = 2;
    /** 状态：失败 */
    public static final int STATUS_FAILED = 3;
    /** 状态：超时 */
    public static final int STATUS_TIMEOUT = 4;
    /** 状态：待审批（敏感内容走人工续跑，一期不自动进入） */
    public static final int STATUS_PENDING_APPROVAL = 5;

    /** 对外任务号（UUID，前端轮询凭据） */
    private String taskId;

    /** 供应商任务号 */
    private String remoteTaskId;

    /** 供应商标识（dashscope 等） */
    private String provider;

    /** 任务类型：image/video */
    private String taskType;

    /** 实际模型名 */
    private String model;

    /** 生成提示词 */
    private String prompt;

    /** 生成参数快照（JSON：size/n/duration 等） */
    private String options;

    /** 来源会话 ID（产物回填对话） */
    private String sessionId;

    /** 来源消息 ID，0=不回填 */
    private Long messageId;

    /** 归属用户 ID */
    private Long userId;

    /** 用户类型：admin/member */
    private String userType;

    /** 租户 ID */
    private Long tenantId;

    /** 状态：0排队 1运行 2成功 3失败 4超时 5待审批 */
    private Integer status;

    /** infra_file.id，0=未转存 */
    private Long fileId;

    /** 产物访问 URL：转存成功写入稳定外链（infra_file），未转存时暂存供应商原始 URL（约 24h 过期） */
    private String resultUrl;

    /** 转存后字节大小 */
    private Long fileSize;

    /** 失败原因 */
    private String errorMsg;

    /** 轮询次数（兜底接管判据） */
    private Integer pollCount;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 结束时间 */
    private LocalDateTime finishTime;
}
