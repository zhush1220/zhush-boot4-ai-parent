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
 * @ClassName UnionShortUrlMap
 * @Description 短链接映射表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_short_url_map")
public class UnionShortUrlMap implements Serializable {

    @Serial
    private static final long serialVersionUID = -7664023142666974281L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 短链码（如：abc123）
     */
    private String shortCode;

    /**
     * 关联推广链接ID
     */
    private Long promotionLinkId;

    /**
     * 原始推广链接
     */
    private String originalUrl;

    /**
     * 最终跳转URL（可做A/B测试或渠道跟踪）
     */
    private String targetUrl;

    /**
     * 过期时间（NULL表示永久）
     */
    private LocalDateTime expireTime;

    /**
     * 总点击量
     */
    private Integer totalClicks;

    /**
     * 独立访客点击量（去重）
     */
    private Integer uniqueClicks;

    /**
     * 最后点击时间
     */
    private LocalDateTime lastClickTime;

    /**
     * 状态：1-正常 2-已禁用 3-已过期
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
