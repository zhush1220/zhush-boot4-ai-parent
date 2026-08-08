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
 * @ClassName UnionWithdrawConfig
 * @Description 提现配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_withdraw_config")
public class UnionWithdrawConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -6257596269819671025L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型：1-数字 2-字符串 3-百分比 4-JSON
     */
    private Integer configType;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
