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
 * @ClassName UnionLinkRequestLog
 * @Description 转链请求日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_link_request_log")
public class UnionLinkRequestLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -5935308378490391866L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 平台
     */
    private Integer platform;

    /**
     * 来源类型：1-商品链接 2-商品ID 3-淘口令 4-搜索关键词
     */
    private Integer sourceType;

    /**
     * 用户输入的原始内容（脱敏后）
     */
    private String sourceInput;

    /**
     * 解析出的第三方商品ID
     */
    private String outerProductId;

    /**
     * 关联union_product.id（可能为NULL，首次转链时商品未入库）
     */
    private Long productId;

    /**
     * 链接类型：1-单品 2-店铺 3-活动 4-通用
     */
    private Integer linkType;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 转链结果：1-成功 2-失败 3-部分成功
     */
    private Integer resultStatus;

    /**
     * 生成的推广链接ID（成功时）
     */
    private Long promotionLinkId;

    /**
     * 生成的推广链接
     */
    private String promotionUrl;

    /**
     * 淘口令/口令码
     */
    private String promotionCode;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 调用第三方API耗时（毫秒）
     */
    private Integer apiCallDuration;

    /**
     * 总耗时（毫秒）
     */
    private Integer totalDuration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
