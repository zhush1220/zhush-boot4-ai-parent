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
 * @ClassName UnionLevelConfig
 * @Description 层级佣金配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_level_config")
public class UnionLevelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -3365635655240711958L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 层级（1/2/3/...）
     */
    private Integer level;

    /**
     * 层级名称（如：合伙人/总监/经理）
     */
    private String levelName;

    /**
     * 该层级的佣金系数（如0.1表示10%）
     */
    private BigDecimal commissionRatio;

    /**
     * 晋升该层级所需最小团队人数
     */
    private Integer minTeamSize;

    /**
     * 晋升该层级所需最小团队佣金
     */
    private BigDecimal minTeamCommission;

    /**
     * 是否启用
     */
    private Integer isActive;

    /**
     * 排序
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
