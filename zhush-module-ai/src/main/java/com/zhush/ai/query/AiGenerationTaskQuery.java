package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 生成任务分页查询（管理端只读列表）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiGenerationTaskQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务类型：image/video */
    private String taskType;

    /** 状态：0排队 1运行 2成功 3失败 4超时 5待审批 */
    private Integer status;

    /** 归属用户 ID（非管理员由服务端强制注入） */
    private Long userId;

    /** 用户类型：admin/member */
    private String userType;
}
