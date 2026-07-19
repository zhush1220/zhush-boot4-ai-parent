package com.zhush.system.controller.admin.vo.role;

import com.zhush.system.entity.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色修改请求（编码不可修改）
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysRole.class)
public class AdminRoleUpdateReqVO {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /** 角色名称 */
    private String name;

    /** 显示顺序 */
    private Integer sort;

    /** 数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义 */
    private Integer dataScope;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 菜单ID列表（非 null 时覆盖更新） */
    private List<Long> menuIds;
}
