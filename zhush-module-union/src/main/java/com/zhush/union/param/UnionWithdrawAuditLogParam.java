package com.zhush.union.param;

import com.zhush.union.entity.UnionWithdrawAuditLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawAuditLogParam
 * @Description 提现审核日志表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionWithdrawAuditLog.class)
@Schema(description = "提现审核日志表新增/修改请求参数")
public class UnionWithdrawAuditLogParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "提现申请ID")
    @NotNull(message = "提现申请ID不能为空")
    private Long applyId;

    @Schema(description = "申请单号")
    @NotBlank(message = "申请单号不能为空")
    private String applyNo;

    @Schema(description = "操作人ID")
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    @NotBlank(message = "操作人姓名不能为空")
    private String operatorName;

    @Schema(description = "操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理")
    @NotNull(message = "操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理不能为空")
    private Integer operationType;

    @Schema(description = "操作前状态")
    private Integer beforeStatus;

    @Schema(description = "操作后状态")
    private Integer afterStatus;

    @Schema(description = "操作备注")
    private String remark;

    @Schema(description = "extra_json")
    private String extraJson;

}
