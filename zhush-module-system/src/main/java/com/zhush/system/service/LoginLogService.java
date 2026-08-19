package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.biz.ip.core.IpRegionUtils;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.system.entity.SysLoginLog;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysLoginLogMapper;
import com.zhush.system.query.LoginLogQuery;
import com.zhush.system.vo.LoginLogVO;
import io.github.linpeilie.Converter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 登录日志服务
 *
 * <p>管理端登录成功 / 失败均记录，独立线程池异步写入 {@code sys_login_log} 表，
 * 不阻塞登录主流程；同时提供管理端的分页查询与详情能力。
 *
 * @author zhush
 */
@Slf4j
@Service
public class LoginLogService {

    /**
     * 登录方式：账号密码
     */
    public static final String LOGIN_TYPE_PASSWORD = "password";

    private final SysLoginLogMapper loginLogMapper;
    private final Converter converter;
    /**
     * 异步落库线程池：单线程顺序写入，队列打满时丢弃并告警（日志不能拖垮登录链路）
     */
    private final ExecutorService logExecutor;

    public LoginLogService(SysLoginLogMapper loginLogMapper, Converter converter) {
        this(loginLogMapper, converter, new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(8192),
                runnable -> {
                    Thread thread = new Thread(runnable, "login-log-writer");
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, executor) -> log.warn("[LoginLog] 日志队列已满，丢弃一条登录日志")));
    }

    /**
     * 测试专用：注入同步执行的 ExecutorService，避免异步断言
     */
    LoginLogService(SysLoginLogMapper loginLogMapper, Converter converter, ExecutorService logExecutor) {
        this.loginLogMapper = loginLogMapper;
        this.converter = converter;
        this.logExecutor = logExecutor;
    }

    /**
     * 记录登录成功
     */
    public void recordLoginSuccess(Long userId, String username, String loginIp,
                                   String userAgent, String device) {
        SysLoginLog loginLog = buildLog(userId, username, loginIp, userAgent, device);
        loginLog.setResultCode(GlobalErrorCode.SUCCESS.getCode());
        loginLog.setResultMsg("登录成功");
        asyncInsert(loginLog);
    }

    /**
     * 记录登录失败（账号不存在 / 密码错误 / 验证码错误 / 禁用等）
     */
    public void recordLoginFailure(String username, String loginIp,
                                   String userAgent, String device, int errorCode, String errorMsg) {
        SysLoginLog loginLog = buildLog(null, username, loginIp, userAgent, device);
        loginLog.setResultCode(errorCode);
        loginLog.setResultMsg(StrUtil.maxLength(errorMsg, 256));
        asyncInsert(loginLog);
    }

    /**
     * 登录日志分页
     */
    public PageResult<LoginLogVO> getLoginLogPage(LoginLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysLoginLog::getUsername, query.getUsername())
                .eq(SysLoginLog::getLoginIp, query.getLoginIp())
                .ge(SysLoginLog::getCreateTime, query.getCreateTimeBegin())
                .le(SysLoginLog::getCreateTime, query.getCreateTimeEnd());
        // 成功/失败过滤：resultCode = 0 表示成功
        if (Boolean.TRUE.equals(query.getSuccess())) {
            queryWrapper.eq(SysLoginLog::getResultCode, GlobalErrorCode.SUCCESS.getCode());
        } else if (Boolean.FALSE.equals(query.getSuccess())) {
            queryWrapper.ne(SysLoginLog::getResultCode, GlobalErrorCode.SUCCESS.getCode());
        }
        queryWrapper.orderBy("id", false);
        Page<SysLoginLog> page = loginLogMapper.selectPageX(query, queryWrapper);
        List<LoginLogVO> list = converter.convert(page.getRecords(), LoginLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 登录日志详情
     */
    public LoginLogVO getLoginLog(Long id) {
        SysLoginLog loginLog = loginLogMapper.selectOneById(id);
        assertNotNull(loginLog, SystemErrorCode.LOGIN_LOG_NOT_FOUND, id);
        return converter.convert(loginLog, LoginLogVO.class);
    }

    // ================ 内部方法 ================

    private SysLoginLog buildLog(Long userId, String username, String loginIp,
                                 String userAgent, String device) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(StrUtil.nullToEmpty(username));
        loginLog.setUserType(LoginTypeConstants.ADMIN);
        loginLog.setLoginIp(StrUtil.nullToEmpty(loginIp));
        loginLog.setLoginRegion(resolveRegion(loginIp));
        loginLog.setUserAgent(StrUtil.maxLength(StrUtil.nullToEmpty(userAgent), 512));
        loginLog.setDevice(StrUtil.blankToDefault(device, "PC"));
        loginLog.setLoginType(LOGIN_TYPE_PASSWORD);
        return loginLog;
    }

    /**
     * IP 归属地（ip2region 离线库；未启用或查询失败时降级为空串）
     */
    private String resolveRegion(String loginIp) {
        try {
            return StrUtil.nullToEmpty(IpRegionUtils.getAddress(loginIp));
        } catch (Exception e) {
            return "";
        }
    }

    private void asyncInsert(SysLoginLog loginLog) {
        logExecutor.execute(() -> {
            try {
                loginLogMapper.insert(loginLog);
            } catch (Exception e) {
                log.error("[LoginLog] 登录日志落库失败, log={}", loginLog, e);
            }
        });
    }

    /**
     * 应用关闭时等待队列中的日志写完
     */
    @PreDestroy
    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
