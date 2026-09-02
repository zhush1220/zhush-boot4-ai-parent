package com.zhush.system.vo;

import com.zhush.system.entity.SysPost;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysPost.class)
@Schema(description = "岗位详情 / 列表返回")
public class AdminPostVO {

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
