package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionWithdrawAuditLog
 * @Description 提现审核日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_withdraw_audit_log")
public class UnionWithdrawAuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -3559925220325919707L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 提现申请ID
     */
    private Long applyId;

    /**
     * 申请单号
     */
    private String applyNo;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理
     */
    private Integer operationType;

    /**
     * 操作前状态
     */
    private Integer beforeStatus;

    /**
     * 操作后状态
     */
    private Integer afterStatus;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * extra_json
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
