package com.zhush.system.controller.admin.vo.role;

import com.zhush.system.entity.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色新增请求
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysRole.class)
public class AdminRoleSaveReqVO {

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 角色编码（如 admin、ops） */
    @NotBlank(message = "角色编码不能为空")
    private String code;

    /** 显示顺序 */
    private Integer sort;

    /** 数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义 */
    private Integer dataScope;

    /** 状态：0-禁用 1-启用（不传默认启用） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 菜单ID列表（授予的权限） */
    private List<Long> menuIds;
}
