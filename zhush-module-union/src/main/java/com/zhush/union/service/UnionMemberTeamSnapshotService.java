package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionMemberTeamSnapshot;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionMemberTeamSnapshotMapper;
import com.zhush.union.param.UnionMemberTeamSnapshotParam;
import com.zhush.union.query.UnionMemberTeamSnapshotQuery;
import com.zhush.union.vo.UnionMemberTeamSnapshotVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionMemberTeamSnapshotService
 * @Description 推广者团队快照表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionMemberTeamSnapshotService {

    private final UnionMemberTeamSnapshotMapper memberTeamSnapshotMapper;
    private final Converter converter;

    public UnionMemberTeamSnapshotService(UnionMemberTeamSnapshotMapper memberTeamSnapshotMapper, Converter converter) {
        this.memberTeamSnapshotMapper = memberTeamSnapshotMapper;
        this.converter = converter;
    }

    /**
     * 新增推广者团队快照
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberTeamSnapshot(UnionMemberTeamSnapshotParam param) {
        UnionMemberTeamSnapshot entity = converter.convert(param, UnionMemberTeamSnapshot.class);
        memberTeamSnapshotMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广者团队快照
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberTeamSnapshot(UnionMemberTeamSnapshotParam param) {
        UnionMemberTeamSnapshot existing = memberTeamSnapshotMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.MEMBER_TEAM_SNAPSHOT_NOT_FOUND, param.getId());
        UnionMemberTeamSnapshot update = converter.convert(param, UnionMemberTeamSnapshot.class);
        memberTeamSnapshotMapper.update(update);
    }

    /**
     * 删除推广者团队快照（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemberTeamSnapshot(Long id) {
        UnionMemberTeamSnapshot existing = memberTeamSnapshotMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.MEMBER_TEAM_SNAPSHOT_NOT_FOUND, id);
        memberTeamSnapshotMapper.deleteById(id);
    }

    /**
     * 推广者团队快照详情
     */
    public UnionMemberTeamSnapshotVO getMemberTeamSnapshot(Long id) {
        UnionMemberTeamSnapshot entity = memberTeamSnapshotMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.MEMBER_TEAM_SNAPSHOT_NOT_FOUND, id);
        return converter.convert(entity, UnionMemberTeamSnapshotVO.class);
    }

    /**
     * 推广者团队快照分页
     */
    public PageResult<UnionMemberTeamSnapshotVO> getMemberTeamSnapshotPage(UnionMemberTeamSnapshotQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionMemberTeamSnapshot::getMemberId, query.getMemberId())
                .eq(UnionMemberTeamSnapshot::getLevel1Count, query.getLevel1Count())
                .eq(UnionMemberTeamSnapshot::getLevel2Count, query.getLevel2Count())
                .eq(UnionMemberTeamSnapshot::getLevel3Count, query.getLevel3Count())
                .eq(UnionMemberTeamSnapshot::getTotalTeamCount, query.getTotalTeamCount())
        ;
        Page<UnionMemberTeamSnapshot> page = memberTeamSnapshotMapper.selectPageX(query, queryWrapper);
        List<UnionMemberTeamSnapshotVO> list = converter.convert(page.getRecords(), UnionMemberTeamSnapshotVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
