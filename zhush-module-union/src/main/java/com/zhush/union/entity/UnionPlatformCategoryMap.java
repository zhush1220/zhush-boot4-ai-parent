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
 * @ClassName UnionPlatformCategoryMap
 * @Description 平台类目映射表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_platform_category_map")
public class UnionPlatformCategoryMap implements Serializable {

    @Serial
    private static final long serialVersionUID = -4773941130690070647L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 平台
     */
    private Integer platform;

    /**
     * 平台类目ID
     */
    private String platformCategoryId;

    /**
     * 平台类目名称
     */
    private String platformCategoryName;

    /**
     * 平台父级类目ID
     */
    private String platformParentId;

    /**
     * 映射到union_category.id
     */
    private Long unionCategoryId;

    /**
     * 1-启用 0-禁用
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
