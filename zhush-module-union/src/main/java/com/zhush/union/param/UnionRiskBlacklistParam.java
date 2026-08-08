package com.zhush.union.param;

import com.zhush.union.entity.UnionRiskBlacklist;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskBlacklistParam
 * @Description 风控黑名单表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionRiskBlacklist.class)
@Schema(description = "风控黑名单表新增/修改请求参数")
public class UnionRiskBlacklistParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号")
    @NotNull(message = "黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号不能为空")
    private Integer blackType;

    @Schema(description = "黑名单值")
    @NotBlank(message = "黑名单值不能为空")
    private String blackValue;

    @Schema(description = "加入原因")
    private String reason;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "过期时间（NULL表示永久）")
    private LocalDateTime expireTime;

    @Schema(description = "1-生效 0-已失效")
    private Integer status;

}
