package com.zhush.system.vo;

import com.zhush.system.entity.InfraFile;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = InfraFile.class)
@Schema(description = "文件详情 / 列表返回")
public class InfraFileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "相对存储路径")
    private String path;

    @Schema(description = "文件访问URL")
    private String url;

    @Schema(description = "文件扩展名")
    private String ext;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "存储类型：local / aliyun-oss / qiniu / huawei-obs")
    private String storageType;

    @Schema(description = "上传人类型：admin-管理端 member-会员端")
    private String userType;

    @Schema(description = "上传人ID")
    private Long userId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
