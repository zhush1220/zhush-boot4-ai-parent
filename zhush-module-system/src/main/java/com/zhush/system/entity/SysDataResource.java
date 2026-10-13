package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据资源登记（哪些业务表/对象纳入行级+列级管控）
 * <p>
 * 元数据镜像自代码中的 {@code @DataPermission} 与 VO 的 {@code @FieldPermission} 注解，
 * 由同步器登记、页面可编辑覆盖列与受控字段开关。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_data_resource")
public class SysDataResource extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 资源编码（全局唯一，如 system:user），对应 @DataPermission.resource */
    private String code;

    /** 资源名称（页面展示，如"管理员用户"） */
    private String name;

    /** 所属模块（如 system、union） */
    private String module;

    /** 业务表名（如 sys_admin_user） */
    private String tableName;

    /** 部门归属列名（镜像 @DataPermission.deptColumn） */
    private String deptColumn;

    /** 用户归属列名（镜像 @DataPermission.userColumn） */
    private String userColumn;

    /** 返回 VO 类全限定名（列级字段扫描依据） */
    private String voClass;

    /** 自定义行级条件允许引用的列白名单（逗号分隔，除 dept/user 列外的可过滤列） */
    private String filterColumns;

    /** 是否纳入行级管控：0-否 1-是 */
    private Integer rowEnabled;

    /** 是否纳入列级管控：0-否 1-是 */
    private Integer fieldEnabled;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
