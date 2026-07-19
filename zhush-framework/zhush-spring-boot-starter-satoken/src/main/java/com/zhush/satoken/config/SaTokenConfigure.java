package com.zhush.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.zhush.satoken.annotation.IgnoreAuth;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
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
 * @author zhush
 */
public class SaTokenConfigure implements WebMvcConfigurer {

    /**
     * 内置免登录白名单（登录/注册/错误页/静态资源/接口文档）
     */
    private static final List<String> DEFAULT_IGNORE_PATHS = List.of(
            "/admin/auth/login",
            "/admin/auth/social-login",
            "/member/auth/login",
            "/member/auth/wx-mini-login",
            "/error",
            "/favicon.ico",
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final SecurityProperties properties;

    public SaTokenConfigure(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> ignorePaths = new ArrayList<>(DEFAULT_IGNORE_PATHS);
        if (properties.getIgnorePaths() != null) {
            ignorePaths.addAll(properties.getIgnorePaths());
        }
        String adminPrefix = properties.getAdminPrefix();
        String memberPrefix = properties.getMemberPrefix();

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

}
