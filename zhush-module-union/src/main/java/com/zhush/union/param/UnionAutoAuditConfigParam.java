package com.zhush.union.param;

import com.zhush.union.entity.UnionAutoAuditConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionAutoAuditConfigParam
 * @Description 自动审核配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionAutoAuditConfig.class)
@Schema(description = "自动审核配置表新增/修改请求参数")
public class UnionAutoAuditConfigParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "描述")
    private String description;

}
