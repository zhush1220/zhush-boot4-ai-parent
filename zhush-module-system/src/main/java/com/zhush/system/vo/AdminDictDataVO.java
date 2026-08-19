package com.zhush.system.vo;

import com.zhush.system.entity.SysDictData;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysDictData.class)
@Schema(description = "字典数据详情 / 列表返回")
public class AdminDictDataVO {

    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "字典标签（展示值）")
    private String label;

    @Schema(description = "字典键值（存储值）")
    private String value;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
