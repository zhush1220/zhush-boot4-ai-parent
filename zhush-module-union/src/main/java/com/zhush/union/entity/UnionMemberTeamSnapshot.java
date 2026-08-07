package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionMemberTeamSnapshot
 * @Description 推广者团队快照表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_member_team_snapshot")
public class UnionMemberTeamSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = -1737707375027604863L;

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
     * 一级下级数
     */
    private Integer level1Count;

    /**
     * 二级下级数
     */
    private Integer level2Count;

    /**
     * 三级下级数
     */
    private Integer level3Count;

    /**
     * 团队总人数
     */
    private Integer totalTeamCount;

    /**
     * 团队累计贡献佣金
     */
    private BigDecimal teamTotalCommission;

    /**
     * 本月团队贡献佣金
     */
    private BigDecimal teamMonthCommission;

    /**
     * 快照日期
     */
    private LocalDate snapshotDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
