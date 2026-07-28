package com.zhush.data.permission.core;

/**
 * 数据范围类型
 *
 * @author zhush
 */
public enum DataScopeType {

    /** 全部数据（不过滤） */
    ALL,

    /** 本部门及以下部门的数据 */
    DEPT_AND_CHILD,

    /** 仅本部门的数据 */
    DEPT,

    /** 仅本人的数据 */
    SELF,

    /** 自定义部门范围的数据 */
    CUSTOM
}
