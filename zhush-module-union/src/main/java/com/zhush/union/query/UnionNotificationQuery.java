package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionNotificationQuery
 * @Description 消息通知表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "消息通知表分页查询请求")
public class UnionNotificationQuery extends BaseQuery {

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

    @Schema(description = "1-正常 2-已撤回")
    private Integer status;

}
