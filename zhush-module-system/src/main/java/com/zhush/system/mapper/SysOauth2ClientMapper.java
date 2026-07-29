package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysOauth2Client;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2 客户端 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysOauth2ClientMapper extends BaseMapperX<SysOauth2Client> {

    /**
     * 按客户端标识查询
     */
    default SysOauth2Client selectByClientId(String clientId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysOauth2Client::getClientId).eq(clientId);
        return selectOneByQuery(queryWrapper);
    }

}
