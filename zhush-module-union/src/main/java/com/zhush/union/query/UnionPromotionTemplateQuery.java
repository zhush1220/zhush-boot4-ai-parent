package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionPromotionTemplateQuery
 * @Description 推广文案模板表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广文案模板表分页查询请求")
public class UnionPromotionTemplateQuery extends BaseQuery {

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "平台（NULL表示通用）")
    private Integer platform;

    @Schema(description = "类目ID（NULL表示通用）")
    private Long categoryId;

    @Schema(description = "模板类型：1-文案 2-海报 3-视频")
    private Integer templateType;

    @Schema(description = "标题模板（支持变量：{title}、{price}、{commission}）")
    private String titleTemplate;

    @Schema(description = "描述模板")
    private String descTemplate;

    @Schema(description = "海报底图URL")
    private String imageUrl;

    @Schema(description = "海报配置（文字位置、颜色、字体大小等）")
    private String imageConfig;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

}
