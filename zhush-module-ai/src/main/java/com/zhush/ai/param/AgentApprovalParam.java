package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 审批处理入参（通过/驳回共用）。
 *
 * @author zhush
 */
@Data
public class AgentApprovalParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "审批编号不能为空")
    private String approvalId;

    /** 通过时改参（可为空） */
    private Map<String, Object> modifiedArgs;

    /** 驳回原因 */
    private String reason;
}
