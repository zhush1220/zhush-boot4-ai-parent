package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 智能体定义分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentDefinitionQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String model;

    private Integer streaming;

    private String groupCode;

    private Integer status;
}
