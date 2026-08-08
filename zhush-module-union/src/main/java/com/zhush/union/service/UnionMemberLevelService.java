package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionMemberLevel;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionMemberLevelMapper;
import com.zhush.union.param.UnionMemberLevelParam;
import com.zhush.union.query.UnionMemberLevelQuery;
import com.zhush.union.vo.UnionMemberLevelVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionMemberLevelService
 * @Description 推广者等级配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionMemberLevelService {

    private final UnionMemberLevelMapper memberLevelMapper;
    private final Converter converter;

    public UnionMemberLevelService(UnionMemberLevelMapper memberLevelMapper, Converter converter) {
        this.memberLevelMapper = memberLevelMapper;
        this.converter = converter;
    }

    /**
     * 新增推广者等级配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberLevel(UnionMemberLevelParam param) {
        UnionMemberLevel entity = converter.convert(param, UnionMemberLevel.class);
        memberLevelMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广者等级配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberLevel(UnionMemberLevelParam param) {
        UnionMemberLevel existing = memberLevelMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.MEMBER_LEVEL_NOT_FOUND, param.getId());
        UnionMemberLevel update = converter.convert(param, UnionMemberLevel.class);
        memberLevelMapper.update(update);
    }

    /**
     * 删除推广者等级配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemberLevel(Long id) {
        UnionMemberLevel existing = memberLevelMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.MEMBER_LEVEL_NOT_FOUND, id);
        memberLevelMapper.deleteById(id);
    }

    /**
     * 推广者等级配置详情
     */
    public UnionMemberLevelVO getMemberLevel(Long id) {
        UnionMemberLevel entity = memberLevelMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.MEMBER_LEVEL_NOT_FOUND, id);
        return converter.convert(entity, UnionMemberLevelVO.class);
    }

    /**
     * 推广者等级配置分页
     */
    public PageResult<UnionMemberLevelVO> getMemberLevelPage(UnionMemberLevelQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionMemberLevel::getLevelCode, query.getLevelCode())
                .like(UnionMemberLevel::getLevelName, query.getLevelName())
                .eq(UnionMemberLevel::getMinTeamSize, query.getMinTeamSize())
                .eq(UnionMemberLevel::getStatus, query.getStatus())
                .eq(UnionMemberLevel::getSortOrder, query.getSortOrder())
        ;
        Page<UnionMemberLevel> page = memberLevelMapper.selectPageX(query, queryWrapper);
        List<UnionMemberLevelVO> list = converter.convert(page.getRecords(), UnionMemberLevelVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
