package com.zhush.system.security;

import cn.dev33.satoken.oauth2.data.loader.SaOAuth2DataLoader;
import cn.dev33.satoken.oauth2.data.model.loader.SaClientModel;
import cn.hutool.core.util.StrUtil;
import com.zhush.system.entity.SysOauth2Client;
import com.zhush.system.mapper.SysOauth2ClientMapper;
import org.springframework.stereotype.Component;

/**
 * OAuth2 客户端数据源
 *
 * <p>授权服务器按 {@code clientId} 回调此处加载客户端信息（来自
 * {@code sys_oauth2_client} 表）。停用 / 删除的客户端返回 null，
 * 协议端点与开放 API 拦截器随即按无效客户端处理，存量令牌立即失效。
 *
 * <p>注意：{@code clientSecret} 放入的是 BCrypt 哈希，
 * 密钥比对由 {@code BCryptSecretOAuth2Template} 适配。
 *
 * @author zhush
 */
@Component
public class Oauth2DataLoaderImpl implements SaOAuth2DataLoader {

    private static final int STATUS_ENABLED = 1;

    private final SysOauth2ClientMapper clientMapper;

    public Oauth2DataLoaderImpl(SysOauth2ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @Override
    public SaClientModel getClientModel(String clientId) {
        SysOauth2Client client = clientMapper.selectByClientId(clientId);
        if (client == null || client.getStatus() == null || client.getStatus() != STATUS_ENABLED) {
            return null;
        }

        SaClientModel model = new SaClientModel()
                .setClientId(client.getClientId())
                .setClientSecret(client.getClientSecret())
                .setContractScopes(StrUtil.splitTrim(client.getScopes(), ','))
                .setAllowRedirectUris(StrUtil.splitTrim(client.getRedirectUris(), ','))
                .setAllowGrantTypes(StrUtil.splitTrim(client.getGrantTypes(), ','))
                .setIsAutoConfirm(client.getAutoConfirm() != null && client.getAutoConfirm() == 1);
        // 客户端级令牌有效期：为空沿用全局默认
        if (client.getAccessTokenTimeout() != null) {
            model.setAccessTokenTimeout(client.getAccessTokenTimeout());
        }
        if (client.getRefreshTokenTimeout() != null) {
            model.setRefreshTokenTimeout(client.getRefreshTokenTimeout());
        }
        return model;
    }

}
