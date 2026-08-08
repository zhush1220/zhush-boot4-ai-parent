package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalWithdraw;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalWithdrawMapper;
import com.zhush.union.param.UnionExternalWithdrawParam;
import com.zhush.union.query.UnionExternalWithdrawQuery;
import com.zhush.union.vo.UnionExternalWithdrawVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalWithdrawService
 * @Description 外部推广者提现申请表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalWithdrawService {

    private final UnionExternalWithdrawMapper externalWithdrawMapper;
    private final Converter converter;

    public UnionExternalWithdrawService(UnionExternalWithdrawMapper externalWithdrawMapper, Converter converter) {
        this.externalWithdrawMapper = externalWithdrawMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者提现申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalWithdraw(UnionExternalWithdrawParam param) {
        UnionExternalWithdraw entity = converter.convert(param, UnionExternalWithdraw.class);
        externalWithdrawMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者提现申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalWithdraw(UnionExternalWithdrawParam param) {
        UnionExternalWithdraw existing = externalWithdrawMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_WITHDRAW_NOT_FOUND, param.getId());
        UnionExternalWithdraw update = converter.convert(param, UnionExternalWithdraw.class);
        externalWithdrawMapper.update(update);
    }

    /**
     * 删除外部推广者提现申请（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalWithdraw(Long id) {
        UnionExternalWithdraw existing = externalWithdrawMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_WITHDRAW_NOT_FOUND, id);
        externalWithdrawMapper.deleteById(id);
    }

    /**
     * 外部推广者提现申请详情
     */
    public UnionExternalWithdrawVO getExternalWithdraw(Long id) {
        UnionExternalWithdraw entity = externalWithdrawMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_WITHDRAW_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalWithdrawVO.class);
    }

    /**
     * 外部推广者提现申请分页
     */
    public PageResult<UnionExternalWithdrawVO> getExternalWithdrawPage(UnionExternalWithdrawQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionExternalWithdraw::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionExternalWithdraw::getMasterMemberId, query.getMasterMemberId())
                .like(UnionExternalWithdraw::getApplyNo, query.getApplyNo())
                .like(UnionExternalWithdraw::getPayAccount, query.getPayAccount())
                .like(UnionExternalWithdraw::getPayAccountName, query.getPayAccountName())
                .eq(UnionExternalWithdraw::getStatus, query.getStatus())
                .eq(UnionExternalWithdraw::getTransactionId, query.getTransactionId())
        ;
        Page<UnionExternalWithdraw> page = externalWithdrawMapper.selectPageX(query, queryWrapper);
        List<UnionExternalWithdrawVO> list = converter.convert(page.getRecords(), UnionExternalWithdrawVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
