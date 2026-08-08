package com.zhush.union.vo;

import com.zhush.union.entity.UnionNotification;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionNotificationVO
 * @Description 消息通知表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionNotification.class)
@Schema(description = "消息通知表详情/列表返回")
public class UnionNotificationVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "接收人ID")
    private Long memberId;

    @Schema(description = "类型：1-提现审核结果 2-佣金到账 3-订单归因 4-系统公告 5-活动推送")
    private Integer notifyType;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "扩展数据（如订单ID、提现ID）")
    private String extraData;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    @Schema(description = "1-正常 2-已撤回")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
