package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色 × 业务对象的行级数据范围覆盖规则
 * <p>
 * 与"角色全局数据范围（sys_role.data_scope）"配合：命中资源覆盖时优先按本表规则过滤，
 * 未配置则回落角色默认。{@code scope_type} 语义与 sys_role.data_scope 一致，另增 6-自定义条件。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_data_resource")
public class SysRoleDataResource extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据范围：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义部门 6-自定义条件 */
    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT = 2;
    public static final int SCOPE_DEPT_AND_CHILD = 3;
    public static final int SCOPE_SELF = 4;
    public static final int SCOPE_CUSTOM_DEPT = 5;
    public static final int SCOPE_CONDITION = 6;

    /** 角色ID */
    private Long roleId;

    /** 资源编码（sys_data_resource.code） */
    private String resourceCode;

    /** 数据范围类型（见 SCOPE_* 常量） */
    private Integer scopeType;

    /** 自定义部门ID集合（逗号分隔，scope_type=5 生效；前端数组↔后端逗号串） */
    private String deptIds;

    /** 自定义条件 JSON（scope_type=6 生效，字段/操作符白名单校验后编译为参数化条件） */
    private String conditionJson;
}
