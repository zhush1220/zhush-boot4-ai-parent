package com.zhush.union.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @ClassName UnionCategory
 * @Description 商品分类表 实体类
 * @Author zhush
 * @Date 2026/8/7 0007 21:24
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(value = "union_category")
public class UnionCategory extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -7653502045413929961L;

    /**
     * 父级ID（0表示顶级）
     */
    @Column("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类图标URL
     */
    private String icon;

    /**
     * 显示顺序
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

}
