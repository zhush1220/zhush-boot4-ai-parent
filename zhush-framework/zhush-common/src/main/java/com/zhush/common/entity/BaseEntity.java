package com.zhush.common.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有实体类的基类
 *
 * @author zhush
 * @since 2026/6/27
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1297080587162331665L;

    /**
     * 主键ID（雪花算法生成）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 创建人ID
     */
    @Column(value = "create_by")
    private Long createBy;

    /**
     * 创建时间
     */
    @Column(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @Column(value = "update_by")
    private Long updateBy;

    /**
     * 更新时间
     */
    @Column(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（true=已删除）
     */
    @Column(value = "is_delete", isLogicDelete = true)
    private Boolean isDelete;
}
