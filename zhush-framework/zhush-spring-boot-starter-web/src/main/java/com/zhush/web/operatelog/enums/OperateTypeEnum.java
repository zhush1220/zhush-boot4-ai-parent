package com.zhush.web.operatelog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum OperateTypeEnum {

    /** 查询 */
    QUERY(1, "查询"),

    /** 新增 */
    CREATE(2, "新增"),

    /** 修改 */
    UPDATE(3, "修改"),

    /** 删除 */
    DELETE(4, "删除"),

    /** 导出 */
    EXPORT(5, "导出"),

    /** 导入 */
    IMPORT(6, "导入"),

    /** 授权（如分配角色、重置密码） */
    GRANT(7, "授权"),

    /** 其它 */
    OTHER(0, "其它");

    /**
     * 类型值（存库）
     */
    private final int type;

    /**
     * 类型描述
     */
    private final String description;

}
