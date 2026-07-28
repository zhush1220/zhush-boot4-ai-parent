package com.zhush.data.permission.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = DataPermissionProperties.PREFIX)
public class DataPermissionProperties {

    public static final String PREFIX = "zhush.data-permission";

    /**
     * 是否启用数据权限（总开关，默认关闭）
     */
    private Boolean enabled = false;

    /**
     * 是否启用行级权限（@DataPermission 自动追加 SQL 过滤，默认开启，受总开关约束）
     */
    private Boolean rowEnabled = true;

    /**
     * 是否启用字段级权限（@FieldPermission 响应置空/脱敏，默认开启，受总开关约束）
     */
    private Boolean fieldEnabled = true;
}
