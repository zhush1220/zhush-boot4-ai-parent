package com.zhush.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.zhush.satoken.annotation.IgnoreAuth;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 拦截器配置
 *
 * <p>鉴权规则：
 * <ul>
 *     <li>命中 {@link IgnoreAuth} 或内置/配置白名单的接口：放行</li>
 *     <li>{@code adminPrefix}（默认 {@code /admin/**}）：校验管理端登录态；权限由 {@code @SaCheckPermission(type=\"admin\")} 注解按需校验</li>
 *     <li>{@code memberPrefix}（默认 {@code /member/**}）：仅校验会员端登录态</li>
 * </ul>
 *
 * <p>路径前缀说明：Sa-Token 路由匹配的是原始 {@code requestURI}（{@code ApplicationInfo.routePrefix} 默认为空，
 * 不会裁剪 context-path），而这里注册的是 Spring MVC 拦截器（其 pattern 已剔除 context-path）。
 * 因此交由 {@link #withContextPath(String)} 统一补全 {@code server.servlet.context-path}，
 * 保证白名单与端前缀在两种写法（带 / 不带 context-path）下均能正确匹配。
 *
 * @author zhush
 */
public class SaTokenConfigure implements WebMvcConfigurer {

    /**
     * 内置免登录白名单（登录/注册/错误页/静态资源/接口文档/OAuth2协议与开放API）
     *
     * <p>此处路径不含 context-path，注册拦截器时由 {@link #withContextPath(String)} 补全
     * （如 context-path=/api 时 {@code /admin/auth/login → /api/admin/auth/login}）。
     *
     * <p>/oauth2/**、/open/** 本就不在 admin/member 前缀的登录校验范围内，
     * 列入白名单作双保险：开放 API 由 OpenApiInterceptor 按 OAuth2 令牌鉴权。
     */
    private static final List<String> DEFAULT_IGNORE_PATHS = List.of(
            "/admin/auth/login",
            "/admin/auth/refresh",
            "/admin/auth/social-login",
            "/member/auth/login",
            "/member/auth/wx-mini-login",
            "/oauth2/**",
            "/open/**",
            "/error",
            "/favicon.ico",
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final SecurityProperties properties;

    /**
     * 已规范化的 Servlet context-path（无 context-path 时为空串，如 {@code /api}）
     */
    private final String contextPath;

    public SaTokenConfigure(SecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.contextPath = normalizeContextPath(environment.getProperty("server.servlet.context-path"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> ignorePaths = new ArrayList<>();
        DEFAULT_IGNORE_PATHS.forEach(path -> ignorePaths.add(withContextPath(path)));
        if (properties.getIgnorePaths() != null) {
            properties.getIgnorePaths().forEach(path -> ignorePaths.add(withContextPath(path)));
        }
        String adminPrefix = withContextPath(properties.getAdminPrefix());
        String memberPrefix = withContextPath(properties.getMemberPrefix());

        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 1. @IgnoreAuth 直接放行
                    if (handler instanceof HandlerMethod handlerMethod
                            && (handlerMethod.hasMethodAnnotation(IgnoreAuth.class)
                            || handlerMethod.getBeanType().isAnnotationPresent(IgnoreAuth.class))) {
                        return;
                    }
                    // 2. 白名单放行；其余按端做登录校验
                    SaRouter.match("/**")
                            .notMatch(ignorePaths)
                            .check(() -> {
                                SaRouter.match(adminPrefix, r -> StpAdminUtil.checkLogin());
                                SaRouter.match(memberPrefix, r -> StpMemberUtil.checkLogin());
                            });
                }))
                .addPathPatterns("/**");
    }

    /**
     * 规范化 context-path：去空白、补前导斜杠、去尾部斜杠；"/" 与空值统一为空串
     */
    private static String normalizeContextPath(String rawContextPath) {
        if (rawContextPath == null || rawContextPath.isBlank()) {
            return "";
        }
        String path = rawContextPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if ("/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * 为 Sa-Token 匹配路径补全 context-path（已带前缀的写法保持不变，可重复调用）
     *
     * @param pattern 原始路径（如 {@code /admin/**}、{@code /admin/auth/login}）
     * @return 含 context-path 的路径（如 {@code /api/admin/**}）
     */
    private String withContextPath(String pattern) {
        if (contextPath.isEmpty() || pattern == null || !pattern.startsWith("/")) {
            return pattern;
        }
        if (pattern.equals(contextPath) || pattern.startsWith(contextPath + "/")) {
            return pattern;
        }
        return contextPath + pattern;
    }

}
