package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class SysDept extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 父部门ID（根为0） */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 显示顺序 */
    private Integer sort;

    /** 负责人用户ID */
    private Long leaderUserId;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
