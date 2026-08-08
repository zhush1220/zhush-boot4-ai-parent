package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionMemberRelation;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionMemberRelationMapper;
import com.zhush.union.param.UnionMemberRelationParam;
import com.zhush.union.query.UnionMemberRelationQuery;
import com.zhush.union.vo.UnionMemberRelationVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionMemberRelationService
 * @Description 推广者关系表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionMemberRelationService {

    private final UnionMemberRelationMapper memberRelationMapper;
    private final Converter converter;

    public UnionMemberRelationService(UnionMemberRelationMapper memberRelationMapper, Converter converter) {
        this.memberRelationMapper = memberRelationMapper;
        this.converter = converter;
    }

    /**
     * 新增推广者关系
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberRelation(UnionMemberRelationParam param) {
        UnionMemberRelation entity = converter.convert(param, UnionMemberRelation.class);
        memberRelationMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广者关系
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRelation(UnionMemberRelationParam param) {
        UnionMemberRelation existing = memberRelationMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.MEMBER_RELATION_NOT_FOUND, param.getId());
        UnionMemberRelation update = converter.convert(param, UnionMemberRelation.class);
        memberRelationMapper.update(update);
    }

    /**
     * 删除推广者关系（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemberRelation(Long id) {
        UnionMemberRelation existing = memberRelationMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.MEMBER_RELATION_NOT_FOUND, id);
        memberRelationMapper.deleteById(id);
    }

    /**
     * 推广者关系详情
     */
    public UnionMemberRelationVO getMemberRelation(Long id) {
        UnionMemberRelation entity = memberRelationMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.MEMBER_RELATION_NOT_FOUND, id);
        return converter.convert(entity, UnionMemberRelationVO.class);
    }

    /**
     * 推广者关系分页
     */
    public PageResult<UnionMemberRelationVO> getMemberRelationPage(UnionMemberRelationQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionMemberRelation::getMemberId, query.getMemberId())
                .eq(UnionMemberRelation::getParentId, query.getParentId())
                .eq(UnionMemberRelation::getRootId, query.getRootId())
                .like(UnionMemberRelation::getPath, query.getPath())
                .eq(UnionMemberRelation::getLevel, query.getLevel())
                .eq(UnionMemberRelation::getMaxDepth, query.getMaxDepth())
                .eq(UnionMemberRelation::getStatus, query.getStatus())
                .eq(UnionMemberRelation::getTotalChildren, query.getTotalChildren())
                .eq(UnionMemberRelation::getTotalTeam, query.getTotalTeam())
                .eq(UnionMemberRelation::getCacheVersion, query.getCacheVersion())
        ;
        Page<UnionMemberRelation> page = memberRelationMapper.selectPageX(query, queryWrapper);
        List<UnionMemberRelationVO> list = converter.convert(page.getRecords(), UnionMemberRelationVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
