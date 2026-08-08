package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionWithdrawApply;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionWithdrawApplyMapper;
import com.zhush.union.param.UnionWithdrawApplyParam;
import com.zhush.union.query.UnionWithdrawApplyQuery;
import com.zhush.union.vo.UnionWithdrawApplyVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionWithdrawApplyService
 * @Description 提现申请表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionWithdrawApplyService {

    private final UnionWithdrawApplyMapper withdrawApplyMapper;
    private final Converter converter;

    public UnionWithdrawApplyService(UnionWithdrawApplyMapper withdrawApplyMapper, Converter converter) {
        this.withdrawApplyMapper = withdrawApplyMapper;
        this.converter = converter;
    }

    /**
     * 新增提现申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWithdrawApply(UnionWithdrawApplyParam param) {
        UnionWithdrawApply entity = converter.convert(param, UnionWithdrawApply.class);
        withdrawApplyMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改提现申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWithdrawApply(UnionWithdrawApplyParam param) {
        UnionWithdrawApply existing = withdrawApplyMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, param.getId());
        UnionWithdrawApply update = converter.convert(param, UnionWithdrawApply.class);
        withdrawApplyMapper.update(update);
    }

    /**
     * 删除提现申请（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawApply(Long id) {
        UnionWithdrawApply existing = withdrawApplyMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, id);
        withdrawApplyMapper.deleteById(id);
    }

    /**
     * 提现申请详情
     */
    public UnionWithdrawApplyVO getWithdrawApply(Long id) {
        UnionWithdrawApply entity = withdrawApplyMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.WITHDRAW_APPLY_NOT_FOUND, id);
        return converter.convert(entity, UnionWithdrawApplyVO.class);
    }

    /**
     * 提现申请分页
     */
    public PageResult<UnionWithdrawApplyVO> getWithdrawApplyPage(UnionWithdrawApplyQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionWithdrawApply::getApplyNo, query.getApplyNo())
                .eq(UnionWithdrawApply::getMemberId, query.getMemberId())
                .like(UnionWithdrawApply::getMemberName, query.getMemberName())
                .like(UnionWithdrawApply::getMemberMobile, query.getMemberMobile())
                .eq(UnionWithdrawApply::getWithdrawType, query.getWithdrawType())
                .like(UnionWithdrawApply::getPayAccount, query.getPayAccount())
                .like(UnionWithdrawApply::getPayAccountName, query.getPayAccountName())
                .like(UnionWithdrawApply::getPayBankName, query.getPayBankName())
                .like(UnionWithdrawApply::getPayBankBranch, query.getPayBankBranch())
                .eq(UnionWithdrawApply::getStatus, query.getStatus())
                .eq(UnionWithdrawApply::getAuditorId, query.getAuditorId())
                .like(UnionWithdrawApply::getAuditorName, query.getAuditorName())
                .like(UnionWithdrawApply::getAuditRemark, query.getAuditRemark())
                .eq(UnionWithdrawApply::getPayOperatorId, query.getPayOperatorId())
                .like(UnionWithdrawApply::getPayOperatorName, query.getPayOperatorName())
                .like(UnionWithdrawApply::getPayChannelOrderNo, query.getPayChannelOrderNo())
                .like(UnionWithdrawApply::getPayFailReason, query.getPayFailReason())
                .eq(UnionWithdrawApply::getRetryCount, query.getRetryCount())
                .eq(UnionWithdrawApply::getTransactionId, query.getTransactionId())
                .like(UnionWithdrawApply::getTransactionNo, query.getTransactionNo())
                .like(UnionWithdrawApply::getCancelReason, query.getCancelReason())
                .like(UnionWithdrawApply::getRemark, query.getRemark())
                .like(UnionWithdrawApply::getExtraJson, query.getExtraJson())
        ;
        Page<UnionWithdrawApply> page = withdrawApplyMapper.selectPageX(query, queryWrapper);
        List<UnionWithdrawApplyVO> list = converter.convert(page.getRecords(), UnionWithdrawApplyVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
