package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置视图对象（列表/详情，apiKey 仅返回掩码）。
 *
 * @author zhush
 */
@Data
public class AiModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 模型编码 */
    private String code;

    /** 展示名 */
    private String name;

    /** 供应商标识 */
    private String type;

    /** 服务地址 */
    private String baseUrl;

    /** 实际模型名 */
    private String model;

    /** 能力类型：text / image / video / audio */
    private String capability;

    /** 是否默认模型：0-否 1-是 */
    private Integer isDefault;

    /** 优先级 */
    private Integer priority;

    /** API Key 掩码（如 sk-****abcd，永不返回明文） */
    private String apiKeyMasked;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
