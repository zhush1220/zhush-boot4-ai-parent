package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 模型配置实体（ai_model）
 *
 * <p>模型配置动态化：启停/默认/能力类型均在管理端操作，运行态经
 * {@code AgentModelResolver} 缓存消费。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_model")
public class AiModelEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模型编码（唯一，Agent 定义 model 字段引用） */
    private String code;

    /** 展示名 */
    private String name;

    /** 供应商标识：dashscope / openai / ollama */
    private String type;

    /** API Key（AES 加密密文，支持 ${ENV} 占位） */
    private String apiKey;

    /** 服务地址（openai 兼容服务） */
    private String baseUrl;

    /** 实际模型名（如 qwen-max、wanx2.1-t2i） */
    private String model;

    /** 能力类型：text-文本 image-图片 video-视频 audio-语音 */
    private String capability;

    /** 输入单价（元/千 token，0=未定价） */
    private BigDecimal inputPrice;

    /** 输出单价（元/千 token，0=未定价） */
    private BigDecimal outputPrice;

    /** 是否默认模型：0-否 1-是（全局唯一） */
    private Integer isDefault;

    /** 优先级（下拉排序，越大越靠前） */
    private Integer priority;

    /** 透传参数（JSON：temperature/top_p/size 等） */
    private String options;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
