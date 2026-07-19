package com.zhush.system.controller.admin.vo.role;

import com.zhush.system.entity.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysRole.class)
public class AdminRoleRespVO {

    /** 角色ID */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 显示顺序 */
    private Integer sort;

    /** 数据权限 */
    private Integer dataScope;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 菜单ID列表 */
    private List<Long> menuIds;
}
