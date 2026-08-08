package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionRiskBlacklist;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionRiskBlacklistMapper;
import com.zhush.union.param.UnionRiskBlacklistParam;
import com.zhush.union.query.UnionRiskBlacklistQuery;
import com.zhush.union.vo.UnionRiskBlacklistVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionRiskBlacklistService
 * @Description 风控黑名单表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionRiskBlacklistService {

    private final UnionRiskBlacklistMapper riskBlacklistMapper;
    private final Converter converter;

    public UnionRiskBlacklistService(UnionRiskBlacklistMapper riskBlacklistMapper, Converter converter) {
        this.riskBlacklistMapper = riskBlacklistMapper;
        this.converter = converter;
    }

    /**
     * 新增风控黑名单
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRiskBlacklist(UnionRiskBlacklistParam param) {
        UnionRiskBlacklist entity = converter.convert(param, UnionRiskBlacklist.class);
        riskBlacklistMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改风控黑名单
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRiskBlacklist(UnionRiskBlacklistParam param) {
        UnionRiskBlacklist existing = riskBlacklistMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.RISK_BLACKLIST_NOT_FOUND, param.getId());
        UnionRiskBlacklist update = converter.convert(param, UnionRiskBlacklist.class);
        riskBlacklistMapper.update(update);
    }

    /**
     * 删除风控黑名单（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRiskBlacklist(Long id) {
        UnionRiskBlacklist existing = riskBlacklistMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.RISK_BLACKLIST_NOT_FOUND, id);
        riskBlacklistMapper.deleteById(id);
    }

    /**
     * 风控黑名单详情
     */
    public UnionRiskBlacklistVO getRiskBlacklist(Long id) {
        UnionRiskBlacklist entity = riskBlacklistMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.RISK_BLACKLIST_NOT_FOUND, id);
        return converter.convert(entity, UnionRiskBlacklistVO.class);
    }

    /**
     * 风控黑名单分页
     */
    public PageResult<UnionRiskBlacklistVO> getRiskBlacklistPage(UnionRiskBlacklistQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionRiskBlacklist::getBlackType, query.getBlackType())
                .like(UnionRiskBlacklist::getBlackValue, query.getBlackValue())
                .like(UnionRiskBlacklist::getReason, query.getReason())
                .eq(UnionRiskBlacklist::getOperatorId, query.getOperatorId())
                .eq(UnionRiskBlacklist::getStatus, query.getStatus())
        ;
        Page<UnionRiskBlacklist> page = riskBlacklistMapper.selectPageX(query, queryWrapper);
        List<UnionRiskBlacklistVO> list = converter.convert(page.getRecords(), UnionRiskBlacklistVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
