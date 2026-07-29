package com.zhush.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysOauth2Client;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysOauth2ClientMapper;
import com.zhush.system.param.Oauth2ClientParam;
import com.zhush.system.query.Oauth2ClientQuery;
import com.zhush.system.vo.Oauth2ClientSecretVO;
import com.zhush.system.vo.Oauth2ClientVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertFalse;
import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * OAuth2 客户端管理服务（CRUD + 密钥重置）
 *
 * <p>client_secret 由服务端随机生成并以 BCrypt 哈希落库，明文仅在创建 / 重置时
 * 回显一次；停用或删除客户端后 {@code Oauth2DataLoaderImpl} 返回 null，
 * 存量令牌在开放 API 拦截器处立即失效。
 *
 * @author zhush
 */
@Service
public class Oauth2ClientService {

    private static final int STATUS_ENABLED = 1;

    private final SysOauth2ClientMapper clientMapper;
    private final Converter converter;

    public Oauth2ClientService(SysOauth2ClientMapper clientMapper, Converter converter) {
        this.clientMapper = clientMapper;
        this.converter = converter;
    }

    /**
     * 新增客户端（返回一次性明文密钥）
     */
    @Transactional(rollbackFor = Exception.class)
    public Oauth2ClientSecretVO createClient(Oauth2ClientParam param) {
        validateClientIdUnique(null, param.getClientId());
        SysOauth2Client client = converter.convert(param, SysOauth2Client.class);
        if (client.getStatus() == null) {
            client.setStatus(STATUS_ENABLED);
        }
        String plainSecret = generateSecret();
        client.setClientSecret(BCrypt.hashpw(plainSecret));
        clientMapper.insert(client);
        return buildSecretVO(client.getId(), client.getClientId(), plainSecret);
    }

    /**
     * 修改客户端（clientId 与密钥不可经此修改）
     */
    public void updateClient(Oauth2ClientParam param) {
        SysOauth2Client existing = clientMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.OAUTH2_CLIENT_NOT_FOUND, param.getId());

        SysOauth2Client update = converter.convert(param, SysOauth2Client.class);
        // 客户端标识创建后不可修改，密钥只能走重置接口
        update.setClientId(null);
        update.setClientSecret(null);
        clientMapper.update(update);
    }

    /**
     * 删除客户端（逻辑删除，存量令牌立即失效）
     */
    public void deleteClient(Long id) {
        SysOauth2Client existing = clientMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.OAUTH2_CLIENT_NOT_FOUND, id);
        clientMapper.deleteById(id);
    }

    /**
     * 重置密钥（返回一次性明文密钥，旧密钥立即作废）
     */
    public Oauth2ClientSecretVO resetSecret(Long id) {
        SysOauth2Client existing = clientMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.OAUTH2_CLIENT_NOT_FOUND, id);

        String plainSecret = generateSecret();
        SysOauth2Client update = new SysOauth2Client();
        update.setId(id);
        update.setClientSecret(BCrypt.hashpw(plainSecret));
        clientMapper.update(update);
        return buildSecretVO(id, existing.getClientId(), plainSecret);
    }

    /**
     * 客户端详情
     */
    public Oauth2ClientVO getClient(Long id) {
        SysOauth2Client client = clientMapper.selectOneById(id);
        assertNotNull(client, SystemErrorCode.OAUTH2_CLIENT_NOT_FOUND, id);
        return converter.convert(client, Oauth2ClientVO.class);
    }

    /**
     * 客户端分页
     */
    public PageResult<Oauth2ClientVO> getClientPage(Oauth2ClientQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysOauth2Client::getClientId, query.getClientId())
                .like(SysOauth2Client::getClientName, query.getClientName())
                .eq(SysOauth2Client::getStatus, query.getStatus());
        queryWrapper.orderBy("id", false);
        Page<SysOauth2Client> page = clientMapper.selectPageX(query, queryWrapper);
        List<Oauth2ClientVO> list = converter.convert(page.getRecords(), Oauth2ClientVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 内部方法 ================

    private void validateClientIdUnique(Long id, String clientId) {
        SysOauth2Client client = clientMapper.selectByClientId(clientId);
        if (client == null) {
            return;
        }
        assertFalse(id == null || !client.getId().equals(id),
                SystemErrorCode.OAUTH2_CLIENT_ID_DUPLICATE, clientId);
    }

    /**
     * 生成随机密钥明文（64 位十六进制，两段 UUID 拼接）
     */
    private String generateSecret() {
        return IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
    }

    private Oauth2ClientSecretVO buildSecretVO(Long id, String clientId, String plainSecret) {
        Oauth2ClientSecretVO vo = new Oauth2ClientSecretVO();
        vo.setId(id);
        vo.setClientId(clientId);
        vo.setClientSecret(plainSecret);
        return vo;
    }

}
