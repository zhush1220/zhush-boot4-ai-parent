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
 * @ClassName UnionMemberLevel
 * @Description 推广者等级配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_member_level")
public class UnionMemberLevel implements Serializable {

    @Serial
    private static final long serialVersionUID = -6788906231929784261L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 等级编码：L1/L2/L3
     */
    private String levelCode;

    /**
     * 等级名称：见习/正式/黄金/钻石/合伙人
     */
    private String levelName;

    /**
     * 晋升所需最小团队人数
     */
    private Integer minTeamSize;

    /**
     * 晋升所需最小团队佣金
     */
    private BigDecimal minTeamCommission;

    /**
     * 佣金系数（等级越高，分配比例越高）
     */
    private BigDecimal commissionMultiplier;

    /**
     * status
     */
    private Integer status;

    /**
     * sort_order
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
