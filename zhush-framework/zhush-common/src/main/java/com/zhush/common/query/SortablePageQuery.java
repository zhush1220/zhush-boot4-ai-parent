package com.zhush.common.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 *
 * @ClassName SortablePageQuery
 * @Description 分页排序查询
 * @Author zhush
 * @Date 2026/7/11 星期六 15:09
 *
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class SortablePageQuery extends BaseQuery{

    private Collection<SortingField> sortingFields;

}
