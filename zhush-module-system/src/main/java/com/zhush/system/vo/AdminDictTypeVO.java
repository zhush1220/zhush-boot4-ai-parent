package com.zhush.system.vo;

import com.zhush.system.entity.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysDictType.class)
@Schema(description = "字典类型详情 / 列表返回")
public class AdminDictTypeVO {

    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "字典类型编码")
    private String type;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
