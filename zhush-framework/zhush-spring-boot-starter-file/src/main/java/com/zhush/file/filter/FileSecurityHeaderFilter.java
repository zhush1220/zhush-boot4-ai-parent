package com.zhush.file.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * 文件直链响应头加固：把「下载后在本地打开」变「在浏览器里打开」之后必须补的一道防线。
 *
 * <p>本地存储经 {@code /files/**} 静态资源映射直出，与后台同域。上传或 AI 产物里的
 * {@code .html}/{@code .svg} 若被直接导航，脚本就在本站源里执行（可取管理端 token）。
 * 这里对可被嗅探成脚本的文本类响应声明 {@code nosniff}，并对 html/svg 追加
 * {@code Content-Security-Policy: sandbox allow-scripts}——脚本仍可在浏览器自己的沙箱里跑，
 * 但处于不透明源，读不到 {@code parent}、localStorage 与 Cookie。
 *
 * <p>刻意不给图片/视频/文档加 {@code nosniff}：这类文件一旦缺少 MIME 映射，
 * nosniff 会让浏览器拒绝渲染（如未登记的 .mov 视频），而嗅探成脚本只对文本类内容有意义。
 *
 * @author zhush
 */
public class FileSecurityHeaderFilter implements Filter {

    /** 可能被嗅探为脚本或内嵌脚本的文本类扩展名 */
    private static final Set<String> SNIFFABLE_TEXT_EXTS = Set.of(
            "html", "htm", "svg", "txt", "md", "csv", "json", "xml", "js", "css");

    /** 需要沙箱化的可执行内容 */
    private static final Set<String> SANDBOX_EXTS = Set.of("html", "htm", "svg");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String ext = extensionOf(httpRequest.getRequestURI());
            if (ext != null && SNIFFABLE_TEXT_EXTS.contains(ext)) {
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                if (SANDBOX_EXTS.contains(ext)) {
                    httpResponse.setHeader("Content-Security-Policy", "sandbox allow-scripts");
                    httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
                }
            }
        }
        chain.doFilter(request, response);
    }

    /** 取 URI 末段扩展名（小写、不含点号）；无扩展名返回 {@code null} */
    private String extensionOf(String uri) {
        if (uri == null) {
            return null;
        }
        int slash = uri.lastIndexOf('/');
        String name = slash < 0 ? uri : uri.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? null : name.substring(dot + 1).toLowerCase();
    }
}
