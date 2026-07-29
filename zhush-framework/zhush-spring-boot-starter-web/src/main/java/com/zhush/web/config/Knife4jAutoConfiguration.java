package com.zhush.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Knife4j / SpringDoc OpenAPI 自动装配
 *
 * <p>装配内容：
 * <ul>
 *     <li>{@link OpenAPI}：全局文档元信息 + Sa-Token 鉴权请求头声明</li>
 *     <li>管理端分组（{@code /admin/**}）与会员端分组（{@code /member/**}）</li>
 * </ul>
 *
 * <p>访问地址：{@code http://ip:port/doc.html}
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(GroupedOpenApi.class)
public class Knife4jAutoConfiguration {

    /**
     * Sa-Token 默认 token 请求头名称
     */
    private static final String TOKEN_HEADER = "satoken";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI zhushOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("zhush-boot4 接口文档")
                        .description("zhush 后台管理系统 API 文档")
                        .version("1.0.0")
                        .contact(new Contact().name("zhush")))
                .components(new Components()
                        .addSecuritySchemes(TOKEN_HEADER, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(TOKEN_HEADER)
                                .description("登录后返回的 token，放入请求头")));
    }

    /**
     * 管理端接口分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端")
                .pathsToMatch("/admin/**")
                .build();
    }

    /**
     * 会员端接口分组
     */
    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("会员端")
                .pathsToMatch("/member/**")
                .build();
    }

    /**
     * 开放平台接口分组（OAuth2 协议端点 + 第三方开放 API）
     */
    @Bean
    public GroupedOpenApi openApi() {
        return GroupedOpenApi.builder()
                .group("开放平台")
                .pathsToMatch("/oauth2/**", "/open/**")
                .build();
    }

}
