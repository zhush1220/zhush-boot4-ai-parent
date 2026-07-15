package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @ClassName SysDatasource
 * @Description 数据源配置实体
 * @Author zhush
 * @Date 2026/7/12 星期日 21:36
 *
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_datasource")
public class SysDatasource extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1818215864984882047L;

    /** 数据源名称(唯一标识) */
    private String name;

    /** JDBC URL */
    private String url;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** JDBC驱动类名 */
    private String driverClass;

    /** 连接池类型 */
    private String poolType;

    /** 初始连接数 */
    private Integer initialSize;

    /** 最大连接数 */
    private Integer maxActive;

    /** 最小空闲连接数 */
    private Integer minIdle;

    /** 获取连接最大等待时间(ms) */
    private Integer maxWait;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
