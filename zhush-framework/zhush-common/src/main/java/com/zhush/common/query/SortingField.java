package com.zhush.common.query;

import lombok.Data;

/**
 *
 * @ClassName SortingField
 * @Description 排序字段
 * @Author zhush
 * @Date 2026/7/11 星期六 15:10
 *
 **/
@Data
public class SortingField {
    private String field;
    private Boolean asc = true;

    public boolean isAsc() {
        return asc != null && asc;
    }
}
