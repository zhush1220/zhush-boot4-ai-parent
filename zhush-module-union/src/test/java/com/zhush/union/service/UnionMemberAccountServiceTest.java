package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.query.BaseQuery;
import com.zhush.common.response.PageResult;
import com.zhush.union.entity.UnionMemberAccount;
import com.zhush.union.mapper.UnionMemberAccountMapper;
import com.zhush.union.param.UnionMemberAccountParam;
import com.zhush.union.query.UnionMemberAccountQuery;
import com.zhush.union.vo.UnionMemberAccountVO;
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
 * 推广者账户资金链路契约测试：CRUD 防呆校验、余额字段转换与分页
 */
@ExtendWith(MockitoExtension.class)
class UnionMemberAccountServiceTest {

    @Mock
    private UnionMemberAccountMapper memberAccountMapper;
    @Mock
    private Converter converter;
    @InjectMocks
    private UnionMemberAccountService memberAccountService;

    private UnionMemberAccount entity(Long id) {
        UnionMemberAccount entity = new UnionMemberAccount();
        entity.setId(id);
        entity.setAvailableBalance(BigDecimal.valueOf(888.88));
        return entity;
    }

    @Test
    void createMemberAccountConvertsAndInserts() {
        UnionMemberAccount entity = entity(100L);
        doReturn(entity).when(converter).<Object, UnionMemberAccount>convert(ArgumentMatchers.<Object>any(), eq(UnionMemberAccount.class));
        UnionMemberAccountParam param = new UnionMemberAccountParam();

        Long id = memberAccountService.createMemberAccount(param);

        assertEquals(100L, id);
        verify(memberAccountMapper).insert(entity);
    }

    @Test
    void updateMemberAccountNotFoundThrows() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(null);
        UnionMemberAccountParam param = new UnionMemberAccountParam();
        param.setId(1L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAccountService.updateMemberAccount(param));

        assertEquals(1003501, ex.getCode());
    }

    @Test
    void updateMemberAccountUpdatesExisting() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionMemberAccount update = entity(1L);
        doReturn(update).when(converter).<Object, UnionMemberAccount>convert(ArgumentMatchers.<Object>any(), eq(UnionMemberAccount.class));
        UnionMemberAccountParam param = new UnionMemberAccountParam();
        param.setId(1L);

        memberAccountService.updateMemberAccount(param);

        verify(memberAccountMapper).update(update);
    }

    @Test
    void deleteMemberAccountNotFoundThrows() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAccountService.deleteMemberAccount(1L));

        assertEquals(1003501, ex.getCode());
    }

    @Test
    void deleteMemberAccountDeletes() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(entity(1L));

        memberAccountService.deleteMemberAccount(1L);

        verify(memberAccountMapper).deleteById(1L);
    }

    @Test
    void getMemberAccountNotFoundThrows() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAccountService.getMemberAccount(1L));

        assertEquals(1003501, ex.getCode());
    }

    @Test
    void getMemberAccountConvertsToVoWithBalance() {
        when(memberAccountMapper.selectOneById(1L)).thenReturn(entity(1L));
        UnionMemberAccountVO vo = new UnionMemberAccountVO();
        doReturn(vo).when(converter).<Object, UnionMemberAccountVO>convert(ArgumentMatchers.<Object>any(), eq(UnionMemberAccountVO.class));

        UnionMemberAccountVO result = memberAccountService.getMemberAccount(1L);

        assertNotNull(result);
    }

    @Test
    void getMemberAccountPageReturnsPageResult() {
        Page<UnionMemberAccount> page = new Page<>();
        page.setRecords(List.of(entity(1L)));
        page.setTotalRow(3);
        when(memberAccountMapper.selectPageX(ArgumentMatchers.<BaseQuery>any(), any())).thenReturn(page);
        UnionMemberAccountVO vo = new UnionMemberAccountVO();
        doReturn(List.of(vo)).when(converter).<Object, UnionMemberAccountVO>convert(anyList(), eq(UnionMemberAccountVO.class));

        PageResult<UnionMemberAccountVO> result =
                memberAccountService.getMemberAccountPage(new UnionMemberAccountQuery());

        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getList().size());
    }
}
