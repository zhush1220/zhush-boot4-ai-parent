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
 * @ClassName UnionProductSyncLog
 * @Description 商品同步日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_product_sync_log")
public class UnionProductSyncLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -8397540163829573343L;

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
     * 同步类型：1-全量 2-增量 3-手动
     */
    private Integer syncType;

    /**
     * 同步总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * start_time
     */
    private LocalDateTime startTime;

    /**
     * end_time
     */
    private LocalDateTime endTime;

    /**
     * 0-进行中 1-成功 2-失败
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
