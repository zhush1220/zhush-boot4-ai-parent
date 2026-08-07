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
 * @ClassName UnionWithdrawRiskRecord
 * @Description 提现风控记录表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_withdraw_risk_record")
public class UnionWithdrawRiskRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = -4987303636201588495L;

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
     * 推广者ID
     */
    private Long memberId;

    /**
     * 风险评分（0-100，越高越危险）
     */
    private Integer riskScore;

    /**
     * 风险等级：1-低 2-中 3-高 4-极高
     */
    private Integer riskLevel;

    /**
     * check_items
     */
    private String checkItems;

    /**
     * hit_rules
     */
    private String hitRules;

    /**
     * 建议：PASS-通过 MANUAL-人工审核 REJECT-拒绝
     */
    private String suggestion;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
