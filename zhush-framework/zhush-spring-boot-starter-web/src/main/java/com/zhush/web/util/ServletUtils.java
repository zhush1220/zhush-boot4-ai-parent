package com.zhush.web.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet 工具类
 *
 * <p>提供当前请求获取、客户端真实 IP 解析、UA 获取等 Web 层常用能力。
 *
 * @author zhush
 */
public final class ServletUtils {

    /**
     * 代理场景下透传客户端 IP 的常见请求头（按优先级排列）
     */
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private static final String UNKNOWN = "unknown";

    private ServletUtils() {
    }

    /**
     * 获取当前线程绑定的请求对象（非 Web 上下文返回 {@code null}）
     */
    public static HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 获取客户端真实 IP（优先解析代理头，多级代理取第一个非 unknown 的 IP）
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能为多级代理链：client, proxy1, proxy2
                int index = ip.indexOf(',');
                return index > 0 ? ip.substring(0, index).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 获取浏览器 UA
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return StrUtil.nullToEmpty(request.getHeader("User-Agent"));
    }

}
