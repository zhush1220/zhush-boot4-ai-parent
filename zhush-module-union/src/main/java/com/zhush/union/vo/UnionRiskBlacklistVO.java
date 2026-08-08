package com.zhush.union.vo;

import com.zhush.union.entity.UnionRiskBlacklist;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskBlacklistVO
 * @Description 风控黑名单表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionRiskBlacklist.class)
@Schema(description = "风控黑名单表详情/列表返回")
public class UnionRiskBlacklistVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号")
    private Integer blackType;

    @Schema(description = "黑名单值")
    private String blackValue;

    @Schema(description = "加入原因")
    private String reason;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "过期时间（NULL表示永久）")
    private LocalDateTime expireTime;

    @Schema(description = "1-生效 0-已失效")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
