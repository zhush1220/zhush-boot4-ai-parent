package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 会话重命名入参。
 *
 * @author zhush
 */
@Data
public class AgentSessionRenameParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 新的会话标题 */
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 100, message = "会话标题长度超出限制")
    private String title;
}
