package com.zhush.union.param;

import com.zhush.union.entity.UnionMemberDevice;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberDeviceParam
 * @Description 推广者设备信息表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionMemberDevice.class)
@Schema(description = "推广者设备信息表新增/修改请求参数")
public class UnionMemberDeviceParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "设备唯一标识（UUID/IMEI/IDFA）")
    @NotBlank(message = "设备唯一标识（UUID/IMEI/IDFA）不能为空")
    private String deviceId;

    @Schema(description = "设备类型：IOS/ANDROID/PC/H5")
    private String deviceType;

    @Schema(description = "设备型号")
    private String deviceModel;

    @Schema(description = "操作系统版本")
    private String osVersion;

    @Schema(description = "APP版本")
    private String appVersion;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "登录次数")
    private Integer loginCount;

    @Schema(description = "1-正常 2-异常 3-已禁用")
    private Integer status;

}
