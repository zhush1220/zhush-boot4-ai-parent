package com.zhush.ai.preview;

/**
 * 服务端转换失败：携带降级语义，供门面把结果翻译成 {@code unsupported} + 文案，
 * 而不是把异常抛给会话链路（方案 §0.1 门禁原则 2「降级不报错」）。
 *
 * @author zhush
 */
public class PreviewConversionException extends RuntimeException {

    /** 失败类型：决定给用户的文案，以及是否值得重试 */
    public enum Kind {
        /** 转换组件未就绪（soffice 不存在 / 未装字体），重试无用 */
        UNAVAILABLE,
        /** 并发或排队超限，稍后可重试 */
        BUSY,
        /** 单文档超时 */
        TIMEOUT,
        /** 转换进程非零退出或未产出 PDF */
        FAILED
    }

    private final Kind kind;

    public PreviewConversionException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public PreviewConversionException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

}
