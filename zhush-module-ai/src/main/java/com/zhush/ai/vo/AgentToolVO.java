package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已注册工具视图对象（工具管理只读）。
 *
 * @author zhush
 */
@Data
public class AgentToolVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    private String beanName;

    private boolean requiresApproval;

    private String groupCode;
}
