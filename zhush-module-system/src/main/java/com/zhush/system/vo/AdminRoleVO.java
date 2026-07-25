package com.zhush.system.vo;

import com.zhush.system.entity.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName AdminRoleVO
 * @Description 角色详情 / 列表返回
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@AutoMapper(target = SysRole.class)
@Schema(description = "角色详情 / 列表返回")
public class AdminRoleVO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "数据权限")
    private Integer dataScope;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

}
