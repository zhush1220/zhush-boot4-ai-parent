package com.zhush.union.param;

import com.zhush.union.entity.UnionAutoAuditConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionAutoAuditConfigParam
 * @Description 自动审核配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionAutoAuditConfig.class)
@Schema(description = "自动审核配置表新增/修改请求参数")
public class UnionAutoAuditConfigParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "配置键")
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "配置值")
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    @Schema(description = "描述")
    private String description;

}
