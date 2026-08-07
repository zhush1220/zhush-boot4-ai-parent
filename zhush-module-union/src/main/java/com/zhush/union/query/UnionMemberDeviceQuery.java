package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionMemberDeviceQuery
 * @Description 推广者设备信息表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广者设备信息表分页查询请求")
public class UnionMemberDeviceQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "设备唯一标识（UUID/IMEI/IDFA）")
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

    @Schema(description = "登录次数")
    private Integer loginCount;

    @Schema(description = "1-正常 2-异常 3-已禁用")
    private Integer status;

}
