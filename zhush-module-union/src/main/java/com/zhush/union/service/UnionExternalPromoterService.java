package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalPromoter;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalPromoterMapper;
import com.zhush.union.param.UnionExternalPromoterParam;
import com.zhush.union.query.UnionExternalPromoterQuery;
import com.zhush.union.vo.UnionExternalPromoterVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalPromoterService
 * @Description 外部推广者/影子推广者表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalPromoterService {

    private final UnionExternalPromoterMapper externalPromoterMapper;
    private final Converter converter;

    public UnionExternalPromoterService(UnionExternalPromoterMapper externalPromoterMapper, Converter converter) {
        this.externalPromoterMapper = externalPromoterMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者/影子推广者
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalPromoter(UnionExternalPromoterParam param) {
        UnionExternalPromoter entity = converter.convert(param, UnionExternalPromoter.class);
        externalPromoterMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者/影子推广者
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalPromoter(UnionExternalPromoterParam param) {
        UnionExternalPromoter existing = externalPromoterMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_PROMOTER_NOT_FOUND, param.getId());
        UnionExternalPromoter update = converter.convert(param, UnionExternalPromoter.class);
        externalPromoterMapper.update(update);
    }

    /**
     * 删除外部推广者/影子推广者（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalPromoter(Long id) {
        UnionExternalPromoter existing = externalPromoterMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_PROMOTER_NOT_FOUND, id);
        externalPromoterMapper.deleteById(id);
    }

    /**
     * 外部推广者/影子推广者详情
     */
    public UnionExternalPromoterVO getExternalPromoter(Long id) {
        UnionExternalPromoter entity = externalPromoterMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_PROMOTER_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalPromoterVO.class);
    }

    /**
     * 外部推广者/影子推广者分页
     */
    public PageResult<UnionExternalPromoterVO> getExternalPromoterPage(UnionExternalPromoterQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionExternalPromoter::getPromoterCode, query.getPromoterCode())
                .like(UnionExternalPromoter::getName, query.getName())
                .like(UnionExternalPromoter::getMobile, query.getMobile())
                .eq(UnionExternalPromoter::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionExternalPromoter::getBindType, query.getBindType())
                .eq(UnionExternalPromoter::getCommissionSettleType, query.getCommissionSettleType())
                .eq(UnionExternalPromoter::getStatus, query.getStatus())
                .like(UnionExternalPromoter::getRemark, query.getRemark())
        ;
        Page<UnionExternalPromoter> page = externalPromoterMapper.selectPageX(query, queryWrapper);
        List<UnionExternalPromoterVO> list = converter.convert(page.getRecords(), UnionExternalPromoterVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
