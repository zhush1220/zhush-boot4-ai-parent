package com.zhush.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 对话任务类型（前端意图 / 配额与用量统计维度）。
 *
 * <p>与 {@code ai_model.capability}（模型能力：text/image/video/audio）是<b>两套语义</b>，不可混用：
 * 前者描述「用户想让 AI 做什么」，后者描述「模型原生能输出什么」。映射关系见 {@link #capability()}。
 *
 * <p>落库口径：{@code ai_chat_message.task_type}、{@code ai_quota_rule.task_type}、
 * {@code ai_usage_record.task_type}、{@code ai_generation_task.task_type}。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum AiTaskType {

    /** 普通对话 */
    CHAT("chat", "通用对话"),
    /** 文案生成 */
    COPYWRITING("copywriting", "文案生成"),
    /** 生成 Word 文档 */
    DOCX("docx", "文档生成"),
    /** 生成 PDF 文档 */
    PDF("pdf", "文档生成"),
    /** 解析上传文档 */
    PARSE("parse", "文档解析"),
    /** 图像生成 */
    IMAGE("image", "图像生成"),
    /** 视频生成 */
    VIDEO("video", "视频生成"),
    ;

    /** 存储值（与前端 capability 选项 value 对齐） */
    private final String code;
    /** 展示名 */
    private final String desc;

    /**
     * 容错解析：未知或空值一律回落为 {@link #CHAT}，避免因前端传参漂移导致落库失败。
     */
    public static AiTaskType of(String code) {
        if (code == null || code.isBlank()) {
            return CHAT;
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(CHAT);
    }

    /**
     * 映射到模型能力维度（{@code ai_model.capability}），供生成类工具按能力取凭证。
     */
    public String capability() {
        return switch (this) {
            case IMAGE -> "image";
            case VIDEO -> "video";
            default -> "text";
        };
    }

    /**
     * 是否为生成类任务（计入 image_count / video_count，且需转存产物）。
     */
    public boolean isGeneration() {
        return this == IMAGE || this == VIDEO;
    }
}
