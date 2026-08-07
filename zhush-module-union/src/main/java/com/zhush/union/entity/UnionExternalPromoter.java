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
 * @ClassName UnionExternalPromoter
 * @Description 外部推广者/影子推广者表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_external_promoter")
public class UnionExternalPromoter implements Serializable {

    @Serial
    private static final long serialVersionUID = -6060026145364629278L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 推广者唯一编码（业务生成）
     */
    private String promoterCode;

    /**
     * 推广者姓名/昵称
     */
    private String name;

    /**
     * 手机号（可选）
     */
    private String mobile;

    /**
     * 所属团长/上级推广者ID（有系统账号）
     */
    private Long masterMemberId;

    /**
     * 绑定类型：1-临时绑定 2-永久绑定 3-活动绑定
     */
    private Integer bindType;

    /**
     * 佣金结算方式：1-由团长手动结算 2-系统自动结算到团长账户
     */
    private Integer commissionSettleType;

    /**
     * 状态：1-正常 2-已禁用 3-已过期
     */
    private Integer status;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * 过期时间（临时绑定用）
     */
    private LocalDateTime expireTime;

    /**
     * 解绑时间
     */
    private LocalDateTime unbindTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
