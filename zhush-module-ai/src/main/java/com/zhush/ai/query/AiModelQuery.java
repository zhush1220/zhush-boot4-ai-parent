package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 模型配置分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;

    private String type;

    private String capability;

    private Integer status;
}
