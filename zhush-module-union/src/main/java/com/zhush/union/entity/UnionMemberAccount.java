package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionMemberAccount
 * @Description 推广者账户表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_member_account")
public class UnionMemberAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = -5550347337339954678L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 累计佣金总额
     */
    private BigDecimal totalCommission;

    /**
     * 可提现余额
     */
    private BigDecimal availableBalance;

    /**
     * 冻结余额（提现中）
     */
    private BigDecimal frozenBalance;

    /**
     * 累计已提现
     */
    private BigDecimal totalWithdrawn;

    /**
     * 待结算佣金（未到结算周期）
     */
    private BigDecimal pendingSettle;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
