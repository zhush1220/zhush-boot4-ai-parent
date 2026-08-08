package com.zhush.union.vo;

import com.zhush.union.entity.UnionWithdrawConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawConfigVO
 * @Description 提现配置表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionWithdrawConfig.class)
@Schema(description = "提现配置表详情/列表返回")
public class UnionWithdrawConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置类型：1-数字 2-字符串 3-百分比 4-JSON")
    private Integer configType;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
