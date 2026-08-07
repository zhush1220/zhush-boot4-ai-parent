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
 * @ClassName UnionProductStatusLog
 * @Description 商品状态变更日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_product_status_log")
public class UnionProductStatusLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -5257596569791412065L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 变更字段：status/is_recommend
     */
    private String fieldName;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String newValue;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 变更原因
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
