package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionWithdrawAuditLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionWithdrawAuditLogMapper;
import com.zhush.union.param.UnionWithdrawAuditLogParam;
import com.zhush.union.query.UnionWithdrawAuditLogQuery;
import com.zhush.union.vo.UnionWithdrawAuditLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionWithdrawAuditLogService
 * @Description 提现审核日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionWithdrawAuditLogService {

    private final UnionWithdrawAuditLogMapper withdrawAuditLogMapper;
    private final Converter converter;

    public UnionWithdrawAuditLogService(UnionWithdrawAuditLogMapper withdrawAuditLogMapper, Converter converter) {
        this.withdrawAuditLogMapper = withdrawAuditLogMapper;
        this.converter = converter;
    }

    /**
     * 新增提现审核日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWithdrawAuditLog(UnionWithdrawAuditLogParam param) {
        UnionWithdrawAuditLog entity = converter.convert(param, UnionWithdrawAuditLog.class);
        withdrawAuditLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改提现审核日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWithdrawAuditLog(UnionWithdrawAuditLogParam param) {
        UnionWithdrawAuditLog existing = withdrawAuditLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.WITHDRAW_AUDIT_LOG_NOT_FOUND, param.getId());
        UnionWithdrawAuditLog update = converter.convert(param, UnionWithdrawAuditLog.class);
        withdrawAuditLogMapper.update(update);
    }

    /**
     * 删除提现审核日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawAuditLog(Long id) {
        UnionWithdrawAuditLog existing = withdrawAuditLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.WITHDRAW_AUDIT_LOG_NOT_FOUND, id);
        withdrawAuditLogMapper.deleteById(id);
    }

    /**
     * 提现审核日志详情
     */
    public UnionWithdrawAuditLogVO getWithdrawAuditLog(Long id) {
        UnionWithdrawAuditLog entity = withdrawAuditLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.WITHDRAW_AUDIT_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionWithdrawAuditLogVO.class);
    }

    /**
     * 提现审核日志分页
     */
    public PageResult<UnionWithdrawAuditLogVO> getWithdrawAuditLogPage(UnionWithdrawAuditLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionWithdrawAuditLog::getApplyId, query.getApplyId())
                .like(UnionWithdrawAuditLog::getApplyNo, query.getApplyNo())
                .eq(UnionWithdrawAuditLog::getOperatorId, query.getOperatorId())
                .like(UnionWithdrawAuditLog::getOperatorName, query.getOperatorName())
                .eq(UnionWithdrawAuditLog::getOperationType, query.getOperationType())
                .eq(UnionWithdrawAuditLog::getBeforeStatus, query.getBeforeStatus())
                .eq(UnionWithdrawAuditLog::getAfterStatus, query.getAfterStatus())
                .like(UnionWithdrawAuditLog::getRemark, query.getRemark())
                .like(UnionWithdrawAuditLog::getExtraJson, query.getExtraJson())
        ;
        Page<UnionWithdrawAuditLog> page = withdrawAuditLogMapper.selectPageX(query, queryWrapper);
        List<UnionWithdrawAuditLogVO> list = converter.convert(page.getRecords(), UnionWithdrawAuditLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
