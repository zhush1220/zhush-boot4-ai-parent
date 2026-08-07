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
 * @ClassName UnionWithdrawWhitelist
 * @Description 提现白名单表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_withdraw_whitelist")
public class UnionWithdrawWhitelist implements Serializable {

    @Serial
    private static final long serialVersionUID = -7137316849489038717L;

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
     * 类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控
     */
    private Integer whitelistType;

    /**
     * 额度上限（NULL表示不限制）
     */
    private BigDecimal limitAmount;

    /**
     * 加入原因
     */
    private String reason;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 过期时间（NULL表示永久）
     */
    private LocalDateTime expireTime;

    /**
     * 1-生效 0-已失效
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
