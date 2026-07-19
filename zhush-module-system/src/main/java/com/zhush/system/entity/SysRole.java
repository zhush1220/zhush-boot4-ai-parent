package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色名称 */
    private String name;

    /** 角色编码（如 admin、ops） */
    private String code;

    /** 显示顺序 */
    private Integer sort;

    /** 数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义 */
    private Integer dataScope;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
