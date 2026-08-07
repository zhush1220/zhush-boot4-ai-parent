package com.zhush.union.param;

import com.zhush.union.entity.UnionWithdrawConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawConfigParam
 * @Description 提现配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionWithdrawConfig.class)
@Schema(description = "提现配置表新增/修改请求参数")
public class UnionWithdrawConfigParam {

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
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

}
