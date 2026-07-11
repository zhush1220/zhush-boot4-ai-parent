package com.zhush.common.query;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @ClassName BaseQuery
 * @Description 分页查询基础类
 * @Author zhush
 * @Date 2026/7/9 星期四 23:25
 *
 **/
@Data
public class BaseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 没有分页，返回所有数据
     */
    public static final Integer PAGE_SIZE_NONE = -1;

    /**
     * 当前页码，默认第一页
     */
    private int pageNum = 1;

    /**
     * 每页大小，默认10条
     */
    private int pageSize = 10;
}
