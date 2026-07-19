package com.zhush.member.social;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 *
 * <p>前缀：{@code zhush.member.wx-mini}
 *
 * @author zhush
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhush.member.wx-mini")
public class WxMaProperties {

    /** 小程序 AppID */
    private String appId;

    /** 小程序 AppSecret */
    private String secret;

}
