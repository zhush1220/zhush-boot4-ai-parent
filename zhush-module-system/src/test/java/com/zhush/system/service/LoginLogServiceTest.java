package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.query.BaseQuery;
import com.zhush.common.response.PageResult;
import com.zhush.system.entity.SysLoginLog;
import com.zhush.system.mapper.SysLoginLogMapper;
import com.zhush.system.query.LoginLogQuery;
import com.zhush.system.vo.LoginLogVO;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
 * 登录日志服务测试：成功/失败记录内容、分页查询、详情防呆
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceTest {

    @Mock
    private SysLoginLogMapper loginLogMapper;
    @Mock
    private Converter converter;

    private ExecutorService executor;
    private LoginLogService loginLogService;

    @BeforeEach
    void setUp() {
        // 同步执行的线程池：execute 直接在当前线程运行，便于断言
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        loginLogService = new LoginLogService(loginLogMapper, converter, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void recordLoginSuccessBuildsLogAndInserts() {
        loginLogService.recordLoginSuccess(1L, "admin", "127.0.0.1", "ua", "PC");

        ArgumentCaptor<SysLoginLog> captor = ArgumentCaptor.forClass(SysLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        SysLoginLog log = captor.getValue();
        assertEquals(1L, log.getUserId());
        assertEquals("admin", log.getUsername());
        assertEquals("admin", log.getUserType());
        assertEquals("127.0.0.1", log.getLoginIp());
        assertEquals("ua", log.getUserAgent());
        assertEquals("PC", log.getDevice());
        assertEquals("password", log.getLoginType());
        assertEquals(0, log.getResultCode());
        assertNotNull(log.getLoginRegion());
    }

    @Test
    void recordLoginFailureBuildsLogWithErrorCode() {
        loginLogService.recordLoginFailure("ghost", "127.0.0.1", null, null, 1001004, "账号或密码错误");

        ArgumentCaptor<SysLoginLog> captor = ArgumentCaptor.forClass(SysLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        SysLoginLog log = captor.getValue();
        // 账号不存在等失败场景 user_id 为 NULL
        assertEquals(null, log.getUserId());
        assertEquals("ghost", log.getUsername());
        assertEquals(1001004, log.getResultCode());
        assertEquals("账号或密码错误", log.getResultMsg());
        // UA 与设备的空值防呆
        assertEquals("", log.getUserAgent());
        assertEquals("PC", log.getDevice());
    }

    @Test
    void insertFailureDoesNotPropagate() {
        when(loginLogMapper.insert(any(SysLoginLog.class)))
                .thenThrow(new RuntimeException("db down"));

        // 落库失败只记错误日志，不抛给登录链路
        loginLogService.recordLoginSuccess(1L, "admin", "127.0.0.1", "ua", "PC");
    }

    @Test
    void getLoginLogPageReturnsPageResult() {
        Page<SysLoginLog> page = new Page<>();
        SysLoginLog entity = new SysLoginLog();
        entity.setId(1L);
        page.setRecords(List.of(entity));
        page.setTotalRow(5);
        when(loginLogMapper.selectPageX(ArgumentMatchers.<BaseQuery>any(), any())).thenReturn(page);
        LoginLogVO vo = new LoginLogVO();
        doReturn(List.of(vo)).when(converter).<Object, LoginLogVO>convert(anyList(), eq(LoginLogVO.class));

        PageResult<LoginLogVO> result = loginLogService.getLoginLogPage(new LoginLogQuery());

        assertEquals(5L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    void getLoginLogNotFoundThrows() {
        when(loginLogMapper.selectOneById(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> loginLogService.getLoginLog(99L));

        assertEquals(1001901, ex.getCode());
    }

}
