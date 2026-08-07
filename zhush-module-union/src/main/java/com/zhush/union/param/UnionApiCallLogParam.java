package com.zhush.union.param;

import com.zhush.union.entity.UnionApiCallLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionApiCallLogParam
 * @Description 平台API调用日志表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionApiCallLog.class)
@Schema(description = "平台API调用日志表新增/修改请求参数")
public class UnionApiCallLogParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "API名称（如：taobao.tbk.item.get）")
    private String apiName;

    @Schema(description = "请求参数（脱敏）")
    private String requestParams;

    @Schema(description = "响应码")
    private String responseCode;

    @Schema(description = "响应消息")
    private String responseMsg;

    @Schema(description = "是否成功")
    private Integer isSuccess;

    @Schema(description = "耗时（毫秒）")
    private Integer duration;

}
