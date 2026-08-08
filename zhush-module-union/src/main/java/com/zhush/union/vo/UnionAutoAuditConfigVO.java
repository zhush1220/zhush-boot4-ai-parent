package com.zhush.union.vo;

import com.zhush.union.entity.UnionAutoAuditConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionAutoAuditConfigVO
 * @Description 自动审核配置表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionAutoAuditConfig.class)
@Schema(description = "自动审核配置表详情/列表返回")
public class UnionAutoAuditConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
