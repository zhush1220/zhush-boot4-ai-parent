package com.zhush.union.mapper;


import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionWithdrawConfig;

/**
 * @ClassName UnionWithdrawConfigMapper
 * @Description 提现配置表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionWithdrawConfigMapper extends BaseMapperX<UnionWithdrawConfig> {

    /**
     * 按配置键取启用中的配置（配置键业务唯一）
     *
     * @param configKey 配置键
     * @return 配置，不存在返回 {@code null}
     */
    default UnionWithdrawConfig selectByConfigKey(String configKey) {
        List<UnionWithdrawConfig> list = selectListByQuery(QueryWrapper.create()
                .where(UnionWithdrawConfig::getConfigKey).eq(configKey)
                .and(UnionWithdrawConfig::getIsActive).eq(true));
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }

}
