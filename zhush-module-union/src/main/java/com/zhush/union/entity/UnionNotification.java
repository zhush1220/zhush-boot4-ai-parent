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
 * @ClassName UnionNotification
 * @Description 消息通知表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_notification")
public class UnionNotification implements Serializable {

    @Serial
    private static final long serialVersionUID = -7645835416973899229L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 接收人ID
     */
    private Long memberId;

    /**
     * 类型：1-提现审核结果 2-佣金到账 3-订单归因 4-系统公告 5-活动推送
     */
    private Integer notifyType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 跳转链接
     */
    private String linkUrl;

    /**
     * 扩展数据（如订单ID、提现ID）
     */
    private String extraData;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 1-正常 2-已撤回
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
