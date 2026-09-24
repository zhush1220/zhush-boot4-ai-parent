package com.zhush.file.core;

/**
 * 文件记录存储：上传字节内容并落库文件记录（{@code infra_file}），返回带主键的结果。
 *
 * <p>存在的意义：{@link FileClient} 是纯存储门面，只写对象存储/本地盘、<b>不写库</b>，
 * 而业务侧（如 AI 对话产物）需要「文件可管理、可清理、可追溯」，必须留下 DB 记录。
 * 由于落库逻辑归属 module-system（{@code infra_file} 表所在模块），而调用方（module-ai）
 * 不应依赖 module-system，故在此定义 SPI 由上层模块提供实现。
 *
 * <p>starter 内置 {@link NoopFileRecordStore} 兜底：仅委托 {@link FileClient#upload}，
 * {@code id} 为 {@code null}（保证未引入 module-system 的独立应用可启动）。
 *
 * @author zhush
 */
public interface FileRecordStore {

    /**
     * 上传并落库（归属人从当前登录上下文解析，仅适用于 Web 请求线程）。
     */
    default FileUploadResult upload(byte[] content, String fileName, String contentType, String userType) {
        return upload(content, fileName, contentType, userType, null);
    }

    /**
     * 上传并落库。
     *
     * @param content     文件字节内容（不能为空）
     * @param fileName    文件名（含扩展名，用于扩展名白名单校验与原始名记录）
     * @param contentType MIME 类型，可为 {@code null}
     * @param userType    归属用户类型：admin / member，决定文件记录的操作者语义，可为空
     * @param userId      归属用户 ID；{@code null} 时由实现方从当前登录上下文解析。
     *                    <b>异步线程（如生成任务转存）必须显式传入</b>，此时无登录上下文
     * @return 上传结果，{@code id} 为文件记录主键（无库实现时为 {@code null}）
     */
    FileUploadResult upload(byte[] content, String fileName, String contentType, String userType, Long userId);
}
