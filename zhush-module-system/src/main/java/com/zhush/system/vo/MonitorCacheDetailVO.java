package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 缓存监控-键详情
 *
 * @author zhush
 */
@Data
@Schema(description = "缓存键详情")
public class MonitorCacheDetailVO {

    @Schema(description = "缓存键")
    private String key;

    @Schema(description = "键类型：string/list/set/zset/hash")
    private String type;

    @Schema(description = "剩余过期时间（秒；-1 表示永不过期）")
    private Long ttl;

    @Schema(description = "value 大小（字节）")
    private Long size;

    @Schema(description = "value 编码（如 raw/json）")
    private String encoding;

    @Schema(description = "value 可读预览（可能被截断）")
    private String valuePreview;

    @Schema(description = "value 是否被截断")
    private Boolean truncated;
}
