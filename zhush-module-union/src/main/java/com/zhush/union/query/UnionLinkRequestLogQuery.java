package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionLinkRequestLogQuery
 * @Description 转链请求日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "转链请求日志表分页查询请求")
public class UnionLinkRequestLogQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "来源类型：1-商品链接 2-商品ID 3-淘口令 4-搜索关键词")
    private Integer sourceType;

    @Schema(description = "用户输入的原始内容（脱敏后）")
    private String sourceInput;

    @Schema(description = "解析出的第三方商品ID")
    private String outerProductId;

    @Schema(description = "关联union_product.id（可能为NULL，首次转链时商品未入库）")
    private Long productId;

    @Schema(description = "链接类型：1-单品 2-店铺 3-活动 4-通用")
    private Integer linkType;

    @Schema(description = "请求IP")
    private String requestIp;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "转链结果：1-成功 2-失败 3-部分成功")
    private Integer resultStatus;

    @Schema(description = "生成的推广链接ID（成功时）")
    private Long promotionLinkId;

    @Schema(description = "生成的推广链接")
    private String promotionUrl;

    @Schema(description = "淘口令/口令码")
    private String promotionCode;

    @Schema(description = "短链接")
    private String shortUrl;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "调用第三方API耗时（毫秒）")
    private Integer apiCallDuration;

    @Schema(description = "总耗时（毫秒）")
    private Integer totalDuration;

}
