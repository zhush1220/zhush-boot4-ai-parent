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
 * @ClassName UnionPromotionTemplate
 * @Description 推广文案模板表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_promotion_template")
public class UnionPromotionTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = -4783118955872976181L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 平台（NULL表示通用）
     */
    private Integer platform;

    /**
     * 类目ID（NULL表示通用）
     */
    private Long categoryId;

    /**
     * 模板类型：1-文案 2-海报 3-视频
     */
    private Integer templateType;

    /**
     * 标题模板（支持变量：{title}、{price}、{commission}）
     */
    private String titleTemplate;

    /**
     * 描述模板
     */
    private String descTemplate;

    /**
     * 海报底图URL
     */
    private String imageUrl;

    /**
     * 海报配置（文字位置、颜色、字体大小等）
     */
    private String imageConfig;

    /**
     * 优先级
     */
    private Integer priority;

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
