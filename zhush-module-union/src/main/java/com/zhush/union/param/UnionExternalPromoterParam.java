package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalPromoter;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalPromoterParam
 * @Description 外部推广者/影子推广者表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionExternalPromoter.class)
@Schema(description = "外部推广者/影子推广者表新增/修改请求参数")
public class UnionExternalPromoterParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "推广者唯一编码（业务生成）")
    private String promoterCode;

    @Schema(description = "推广者姓名/昵称")
    private String name;

    @Schema(description = "手机号（可选）")
    private String mobile;

    @Schema(description = "所属团长/上级推广者ID（有系统账号）")
    private Long masterMemberId;

    @Schema(description = "绑定类型：1-临时绑定 2-永久绑定 3-活动绑定")
    private Integer bindType;

    @Schema(description = "佣金结算方式：1-由团长手动结算 2-系统自动结算到团长账户")
    private Integer commissionSettleType;

    @Schema(description = "状态：1-正常 2-已禁用 3-已过期")
    private Integer status;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;

    @Schema(description = "过期时间（临时绑定用）")
    private LocalDateTime expireTime;

    @Schema(description = "解绑时间")
    private LocalDateTime unbindTime;

    @Schema(description = "备注")
    private String remark;

}
