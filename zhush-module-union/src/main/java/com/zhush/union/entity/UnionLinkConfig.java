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
 * @ClassName UnionLinkConfig
 * @Description 转链配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_link_config")
public class UnionLinkConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -6842657183222843491L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 推广者等级（NULL表示全部）
     */
    private Integer memberLevel;

    /**
     * 平台（NULL表示全部）
     */
    private Integer platform;

    /**
     * 类目ID（NULL表示全部）
     */
    private Long categoryId;

    /**
     * 是否生成短链接
     */
    private Boolean enableShortUrl;

    /**
     * 短链接有效期（天数）
     */
    private Integer shortUrlExpireDays;

    /**
     * 是否生成口令/二维码
     */
    private Boolean enablePromotionCode;

    /**
     * 是否生成海报
     */
    private Boolean enablePoster;

    /**
     * 转链时自动保存商品信息到union_product
     */
    private Boolean autoSaveProduct;

    /**
     * 是否启用点击追踪
     */
    private Boolean clickTrackingEnabled;

    /**
     * 是否启用智能跳转（根据设备跳转不同端）
     */
    private Boolean smartRedirect;

    /**
     * expire_strategy
     */
    private String expireStrategy;

    /**
     * 每分钟限流次数（0表示不限）
     */
    private Integer rateLimitPerMinute;

    /**
     * 每小时限流次数（0表示不限）
     */
    private Integer rateLimitPerHour;

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
