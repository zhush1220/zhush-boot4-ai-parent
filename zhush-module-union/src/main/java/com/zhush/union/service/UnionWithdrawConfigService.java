package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionWithdrawConfig;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionWithdrawConfigMapper;
import com.zhush.union.param.UnionWithdrawConfigParam;
import com.zhush.union.query.UnionWithdrawConfigQuery;
import com.zhush.union.vo.UnionWithdrawConfigVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionWithdrawConfigService
 * @Description 提现配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionWithdrawConfigService {

    private final UnionWithdrawConfigMapper withdrawConfigMapper;
    private final Converter converter;

    public UnionWithdrawConfigService(UnionWithdrawConfigMapper withdrawConfigMapper, Converter converter) {
        this.withdrawConfigMapper = withdrawConfigMapper;
        this.converter = converter;
    }

    /**
     * 新增提现配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWithdrawConfig(UnionWithdrawConfigParam param) {
        UnionWithdrawConfig entity = converter.convert(param, UnionWithdrawConfig.class);
        withdrawConfigMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改提现配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWithdrawConfig(UnionWithdrawConfigParam param) {
        UnionWithdrawConfig existing = withdrawConfigMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.WITHDRAW_CONFIG_NOT_FOUND, param.getId());
        UnionWithdrawConfig update = converter.convert(param, UnionWithdrawConfig.class);
        withdrawConfigMapper.update(update);
    }

    /**
     * 删除提现配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawConfig(Long id) {
        UnionWithdrawConfig existing = withdrawConfigMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.WITHDRAW_CONFIG_NOT_FOUND, id);
        withdrawConfigMapper.deleteById(id);
    }

    /**
     * 提现配置详情
     */
    public UnionWithdrawConfigVO getWithdrawConfig(Long id) {
        UnionWithdrawConfig entity = withdrawConfigMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.WITHDRAW_CONFIG_NOT_FOUND, id);
        return converter.convert(entity, UnionWithdrawConfigVO.class);
    }

    /**
     * 提现配置分页
     */
    public PageResult<UnionWithdrawConfigVO> getWithdrawConfigPage(UnionWithdrawConfigQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionWithdrawConfig::getConfigKey, query.getConfigKey())
                .like(UnionWithdrawConfig::getConfigName, query.getConfigName())
                .like(UnionWithdrawConfig::getConfigValue, query.getConfigValue())
                .eq(UnionWithdrawConfig::getConfigType, query.getConfigType())
                .like(UnionWithdrawConfig::getDescription, query.getDescription())
                .eq(UnionWithdrawConfig::getSortOrder, query.getSortOrder())
        ;
        Page<UnionWithdrawConfig> page = withdrawConfigMapper.selectPageX(query, queryWrapper);
        List<UnionWithdrawConfigVO> list = converter.convert(page.getRecords(), UnionWithdrawConfigVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
