package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionWithdrawRiskRecord;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionWithdrawRiskRecordMapper;
import com.zhush.union.param.UnionWithdrawRiskRecordParam;
import com.zhush.union.query.UnionWithdrawRiskRecordQuery;
import com.zhush.union.vo.UnionWithdrawRiskRecordVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionWithdrawRiskRecordService
 * @Description 提现风控记录表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionWithdrawRiskRecordService {

    private final UnionWithdrawRiskRecordMapper withdrawRiskRecordMapper;
    private final Converter converter;

    public UnionWithdrawRiskRecordService(UnionWithdrawRiskRecordMapper withdrawRiskRecordMapper, Converter converter) {
        this.withdrawRiskRecordMapper = withdrawRiskRecordMapper;
        this.converter = converter;
    }

    /**
     * 新增提现风控记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWithdrawRiskRecord(UnionWithdrawRiskRecordParam param) {
        UnionWithdrawRiskRecord entity = converter.convert(param, UnionWithdrawRiskRecord.class);
        withdrawRiskRecordMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改提现风控记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWithdrawRiskRecord(UnionWithdrawRiskRecordParam param) {
        UnionWithdrawRiskRecord existing = withdrawRiskRecordMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.WITHDRAW_RISK_RECORD_NOT_FOUND, param.getId());
        UnionWithdrawRiskRecord update = converter.convert(param, UnionWithdrawRiskRecord.class);
        withdrawRiskRecordMapper.update(update);
    }

    /**
     * 删除提现风控记录（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawRiskRecord(Long id) {
        UnionWithdrawRiskRecord existing = withdrawRiskRecordMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.WITHDRAW_RISK_RECORD_NOT_FOUND, id);
        withdrawRiskRecordMapper.deleteById(id);
    }

    /**
     * 提现风控记录详情
     */
    public UnionWithdrawRiskRecordVO getWithdrawRiskRecord(Long id) {
        UnionWithdrawRiskRecord entity = withdrawRiskRecordMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.WITHDRAW_RISK_RECORD_NOT_FOUND, id);
        return converter.convert(entity, UnionWithdrawRiskRecordVO.class);
    }

    /**
     * 提现风控记录分页
     */
    public PageResult<UnionWithdrawRiskRecordVO> getWithdrawRiskRecordPage(UnionWithdrawRiskRecordQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionWithdrawRiskRecord::getApplyId, query.getApplyId())
                .eq(UnionWithdrawRiskRecord::getMemberId, query.getMemberId())
                .eq(UnionWithdrawRiskRecord::getRiskScore, query.getRiskScore())
                .eq(UnionWithdrawRiskRecord::getRiskLevel, query.getRiskLevel())
                .like(UnionWithdrawRiskRecord::getCheckItems, query.getCheckItems())
                .like(UnionWithdrawRiskRecord::getHitRules, query.getHitRules())
                .like(UnionWithdrawRiskRecord::getSuggestion, query.getSuggestion())
        ;
        Page<UnionWithdrawRiskRecord> page = withdrawRiskRecordMapper.selectPageX(query, queryWrapper);
        List<UnionWithdrawRiskRecordVO> list = converter.convert(page.getRecords(), UnionWithdrawRiskRecordVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
