package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionRiskBlacklistQuery
 * @Description 风控黑名单表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "风控黑名单表分页查询请求")
public class UnionRiskBlacklistQuery extends BaseQuery {

    @Schema(description = "黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号")
    private Integer blackType;

    @Schema(description = "黑名单值")
    private String blackValue;

    @Schema(description = "加入原因")
    private String reason;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "1-生效 0-已失效")
    private Integer status;

}
