package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.query.BaseQuery;
import com.zhush.common.response.PageResult;
import com.zhush.union.entity.UnionWithdrawApply;
import com.zhush.union.mapper.UnionWithdrawApplyMapper;
import com.zhush.union.param.UnionWithdrawApplyParam;
import com.zhush.union.query.UnionWithdrawApplyQuery;
import com.zhush.union.vo.UnionWithdrawApplyVO;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提现申请资金链路契约测试：CRUD 防呆校验、转换与分页
 */
@ExtendWith(MockitoExtension.class)
class UnionWithdrawApplyServiceTest {

    @Mock
    private UnionWithdrawApplyMapper withdrawApplyMapper;
    @Mock
    private Converter converter;
    @InjectMocks
    private UnionWithdrawApplyService withdrawApplyService;

    private UnionWithdrawApply entity(Long id) {
        UnionWithdrawApply entity = new UnionWithdrawApply();
        entity.setId(id);
        entity.setApplyAmount(BigDecimal.valueOf(99.5));
        return entity;
    }

    @Test
    void createWithdrawApplyConvertsAndInserts() {
        UnionWithdrawApply entity = entity(100L);
        doReturn(entity).when(converter).<Object, UnionWithdrawApply>convert(ArgumentMatchers.<Object>any(), eq(UnionWithdrawApply.class));
        UnionWithdrawApplyParam param = new UnionWithdrawApplyParam();

        Long id = withdrawApplyService.createWithdrawApply(param);

        assertEquals(100L, id);
        verify(withdrawApplyMapper).insert(entity);
    }

    @Test
    void updateWithdrawApplyNotFoundThrows() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(null);
        UnionWithdrawApplyParam param = new UnionWithdrawApplyParam();
        param.setId(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> withdrawApplyService.updateWithdrawApply(param));

        assertEquals(1003701, ex.getCode());
    }

    @Test
    void updateWithdrawApplyUpdatesExisting() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionWithdrawApply update = entity(1L);
        doReturn(update).when(converter).<Object, UnionWithdrawApply>convert(ArgumentMatchers.<Object>any(), eq(UnionWithdrawApply.class));
        UnionWithdrawApplyParam param = new UnionWithdrawApplyParam();
        param.setId(1L);

        withdrawApplyService.updateWithdrawApply(param);

        verify(withdrawApplyMapper).update(update);
    }

    @Test
    void deleteWithdrawApplyNotFoundThrows() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> withdrawApplyService.deleteWithdrawApply(1L));

        assertEquals(1003701, ex.getCode());
    }

    @Test
    void deleteWithdrawApplyDeletes() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(entity(1L));

        withdrawApplyService.deleteWithdrawApply(1L);

        verify(withdrawApplyMapper).deleteById(1L);
    }

    @Test
    void getWithdrawApplyNotFoundThrows() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> withdrawApplyService.getWithdrawApply(1L));

        assertEquals(1003701, ex.getCode());
    }

    @Test
    void getWithdrawApplyConvertsToVo() {
        when(withdrawApplyMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionWithdrawApplyVO vo = new UnionWithdrawApplyVO();
        doReturn(vo).when(converter).<Object, UnionWithdrawApplyVO>convert(ArgumentMatchers.<Object>any(), eq(UnionWithdrawApplyVO.class));

        UnionWithdrawApplyVO result = withdrawApplyService.getWithdrawApply(1L);

        assertNotNull(result);
    }

    @Test
    void getWithdrawApplyPageReturnsPageResult() {
        Page<UnionWithdrawApply> page = new Page<>();
        page.setRecords(List.of(entity(1L)));
        page.setTotalRow(2);
        when(withdrawApplyMapper.selectPageX(ArgumentMatchers.<BaseQuery>any(), any())).thenReturn(page);
        UnionWithdrawApplyVO vo = new UnionWithdrawApplyVO();
        doReturn(List.of(vo)).when(converter).<Object, UnionWithdrawApplyVO>convert(anyList(), eq(UnionWithdrawApplyVO.class));

        PageResult<UnionWithdrawApplyVO> result =
                withdrawApplyService.getWithdrawApplyPage(new UnionWithdrawApplyQuery());

        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getList().size());
    }
}
