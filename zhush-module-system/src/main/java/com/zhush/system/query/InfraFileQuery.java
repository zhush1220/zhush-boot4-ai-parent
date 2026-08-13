package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 文件分页查询
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件分页查询")
public class InfraFileQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "原始文件名（模糊匹配）")
    private String name;

    @Schema(description = "存储类型：local / aliyun-oss / qiniu / huawei-obs")
    private String storageType;

    @Schema(description = "上传人类型：admin-管理端 member-会员端")
    private String userType;

}
