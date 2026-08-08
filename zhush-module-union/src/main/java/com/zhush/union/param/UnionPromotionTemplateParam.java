package com.zhush.union.param;

import com.zhush.union.entity.UnionPromotionTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionPromotionTemplateParam
 * @Description 推广文案模板表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionPromotionTemplate.class)
@Schema(description = "推广文案模板表新增/修改请求参数")
public class UnionPromotionTemplateParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "平台（NULL表示通用）")
    private Integer platform;

    @Schema(description = "类目ID（NULL表示通用）")
    private Long categoryId;

    @Schema(description = "模板类型：1-文案 2-海报 3-视频")
    @NotNull(message = "模板类型：1-文案 2-海报 3-视频不能为空")
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
