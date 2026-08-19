package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.query.BaseQuery;
import com.zhush.common.response.PageResult;
import com.zhush.union.entity.UnionCommissionRecord;
import com.zhush.union.mapper.UnionCommissionRecordMapper;
import com.zhush.union.param.UnionCommissionRecordParam;
import com.zhush.union.query.UnionCommissionRecordQuery;
import com.zhush.union.vo.UnionCommissionRecordVO;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * 佣金记录资金链路契约测试：CRUD 防呆校验（不存在必报错）、转换与分页
 */
@ExtendWith(MockitoExtension.class)
class UnionCommissionRecordServiceTest {

    @Mock
    private UnionCommissionRecordMapper commissionRecordMapper;
    @Mock
    private Converter converter;
    @InjectMocks
    private UnionCommissionRecordService commissionRecordService;

    private UnionCommissionRecord entity(Long id) {
        UnionCommissionRecord entity = new UnionCommissionRecord();
        entity.setId(id);
        return entity;
    }

    @Test
    void createCommissionRecordConvertsAndInserts() {
        UnionCommissionRecord entity = entity(100L);
        doReturn(entity).when(converter).<Object, UnionCommissionRecord>convert(ArgumentMatchers.<Object>any(), eq(UnionCommissionRecord.class));
        UnionCommissionRecordParam param = new UnionCommissionRecordParam();

        Long id = commissionRecordService.createCommissionRecord(param);

        assertEquals(100L, id);
        verify(commissionRecordMapper).insert(entity);
    }

    @Test
    void updateCommissionRecordNotFoundThrows() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(null);
        UnionCommissionRecordParam param = new UnionCommissionRecordParam();
        param.setId(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> commissionRecordService.updateCommissionRecord(param));

        assertEquals(1003301, ex.getCode());
    }

    @Test
    void updateCommissionRecordUpdatesExisting() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionCommissionRecord update = entity(1L);
        doReturn(update).when(converter).<Object, UnionCommissionRecord>convert(ArgumentMatchers.<Object>any(), eq(UnionCommissionRecord.class));
        UnionCommissionRecordParam param = new UnionCommissionRecordParam();
        param.setId(1L);

        commissionRecordService.updateCommissionRecord(param);

        verify(commissionRecordMapper).update(update);
    }

    @Test
    void deleteCommissionRecordNotFoundThrows() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> commissionRecordService.deleteCommissionRecord(1L));

        assertEquals(1003301, ex.getCode());
    }

    @Test
    void deleteCommissionRecordDeletes() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(entity(1L));

        commissionRecordService.deleteCommissionRecord(1L);

        verify(commissionRecordMapper).deleteById(1L);
    }

    @Test
    void getCommissionRecordNotFoundThrows() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> commissionRecordService.getCommissionRecord(1L));

        assertEquals(1003301, ex.getCode());
    }

    @Test
    void getCommissionRecordConvertsToVo() {
        when(commissionRecordMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionCommissionRecordVO vo = new UnionCommissionRecordVO();
        doReturn(vo).when(converter).<Object, UnionCommissionRecordVO>convert(ArgumentMatchers.<Object>any(), eq(UnionCommissionRecordVO.class));

        UnionCommissionRecordVO result = commissionRecordService.getCommissionRecord(1L);

        assertNotNull(result);
    }

    @Test
    void getCommissionRecordPageReturnsPageResult() {
        Page<UnionCommissionRecord> page = new Page<>();
        page.setRecords(List.of(entity(1L)));
        page.setTotalRow(1);
        when(commissionRecordMapper.selectPageX(ArgumentMatchers.<BaseQuery>any(), any())).thenReturn(page);
        UnionCommissionRecordVO vo = new UnionCommissionRecordVO();
        doReturn(List.of(vo)).when(converter).<Object, UnionCommissionRecordVO>convert(anyList(), eq(UnionCommissionRecordVO.class));

        PageResult<UnionCommissionRecordVO> result =
                commissionRecordService.getCommissionRecordPage(new UnionCommissionRecordQuery());

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }
}
