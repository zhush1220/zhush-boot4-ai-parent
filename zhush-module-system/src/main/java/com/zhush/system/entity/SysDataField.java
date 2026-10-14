package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 受控字段元数据（列级数据权限的可授权字段清单）
 * <p>
 * 由同步器扫描 VO 的 {@code @FieldPermission} 自动生成，权限点复用 sys_menu（type=3 按钮），
 * 授权仍走角色菜单树，不建第二套授权存储；本表仅提供"字段 ↔ 权限点"的映射元数据。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_data_field")
public class SysDataField extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属资源ID（sys_data_resource.id） */
    private Long resourceId;

    /** 资源编码（冗余，便于按编码查询） */
    private String resourceCode;

    /** VO 字段路径（如 mobile、loginIp） */
    private String fieldPath;

    /** 字段中文名（页面展示） */
    private String label;

    /** 权限标识（如 system:user:mobile），与 sys_menu.permission 对应 */
    private String permissionCode;

    /** 关联的 sys_menu 按钮ID（授权即分配该菜单） */
    private Long menuId;

    /** 默认策略：CLEAR-置空 MASK-脱敏（镜像 @FieldPermission.strategy） */
    private String defaultStrategy;

    /** 脱敏占位值（镜像 @FieldPermission.maskValue） */
    private String maskValue;

    /** 是否纳入列级管控：0-否（放行） 1-是（受控） */
    private Integer status;
}
