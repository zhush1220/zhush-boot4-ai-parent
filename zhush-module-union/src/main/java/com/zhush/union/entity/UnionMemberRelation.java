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
 * @ClassName UnionMemberRelation
 * @Description 推广者关系表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_member_relation")
public class UnionMemberRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = -1067117696431477692L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 推广者ID（本人）
     */
    private Long memberId;

    /**
     * 上级推广者ID（直接邀请人）
     */
    private Long parentId;

    /**
     * 顶级推广者ID（根节点，用于快速归属）
     */
    private Long rootId;

    /**
     * 层级路径：例如 /1/23/45/ ，从根节点到本人的ID路径
     */
    private String path;

    /**
     * 当前层级：1表示顶级，2表示二级，以此类推
     */
    private Integer level;

    /**
     * 该节点以下的最大深度（子树深度，便于缓存）
     */
    private Integer maxDepth;

    /**
     * 状态：1-正常 2-已解除 3-已冻结
     */
    private Integer status;

    /**
     * 绑定时间（成为下级的时间）
     */
    private LocalDateTime bindTime;

    /**
     * 解绑时间
     */
    private LocalDateTime unbindTime;

    /**
     * 直属下级数量
     */
    private Integer totalChildren;

    /**
     * 整个团队人数（所有层级）
     */
    private Integer totalTeam;

    /**
     * 缓存版本号（变更时自增）
     */
    private Integer cacheVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
