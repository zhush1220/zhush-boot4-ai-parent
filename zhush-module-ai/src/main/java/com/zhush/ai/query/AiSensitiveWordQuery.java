package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 敏感词分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiSensitiveWordQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String word;

    private Integer level;

    private Integer status;

    private String groupCode;
}
