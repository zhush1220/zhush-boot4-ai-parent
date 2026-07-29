package com.zhush.system.service;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.oauth2.core.OpenApiCallRecord;
import com.zhush.oauth2.core.OpenApiLogRecorder;
import com.zhush.system.entity.SysOpenApiLog;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysOpenApiLogMapper;
import com.zhush.system.query.OpenApiLogQuery;
import com.zhush.system.vo.OpenApiLogVO;
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
 * 开放 API 调用日志服务
 *
 * <p>实现框架层 {@link OpenApiLogRecorder}：开放 API 拦截器请求完成后回调，
 * 交给独立线程池异步写入 {@code sys_open_api_log} 表，不阻塞开放接口主流程；
 * 同时提供管理端的分页查询与详情能力。
 *
 * @author zhush
 */
@Slf4j
@Service
public class OpenApiLogService implements OpenApiLogRecorder {

    /**
     * 异步落库线程池：单线程顺序写入，队列打满时丢弃并告警（日志不能拖垮业务）
     */
    private final ExecutorService logExecutor = new ThreadPoolExecutor(
            1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8192),
            runnable -> {
                Thread thread = new Thread(runnable, "open-api-log-writer");
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> log.warn("[OpenApiLog] 日志队列已满，丢弃一条调用日志"));

    private final SysOpenApiLogMapper openApiLogMapper;
    private final Converter converter;

    public OpenApiLogService(SysOpenApiLogMapper openApiLogMapper, Converter converter) {
        this.openApiLogMapper = openApiLogMapper;
        this.converter = converter;
    }

    /**
     * 记录一次开放 API 调用：异步落库
     */
    @Override
    public void record(OpenApiCallRecord record) {
        SysOpenApiLog openApiLog = BeanUtil.copyProperties(record, SysOpenApiLog.class);
        logExecutor.execute(() -> {
            try {
                openApiLogMapper.insert(openApiLog);
            } catch (Exception e) {
                log.error("[OpenApiLog] 调用日志落库失败, log={}", openApiLog, e);
            }
        });
    }

    /**
     * 调用日志分页
     */
    public PageResult<OpenApiLogVO> getOpenApiLogPage(OpenApiLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysOpenApiLog::getClientId, query.getClientId())
                .eq(SysOpenApiLog::getLoginId, query.getLoginId())
                .like(SysOpenApiLog::getRequestUri, query.getRequestUri())
                .eq(SysOpenApiLog::getStatus, query.getStatus())
                .eq(SysOpenApiLog::getErrorCode, query.getErrorCode())
                .ge(SysOpenApiLog::getCallTime, query.getCallTimeBegin())
                .le(SysOpenApiLog::getCallTime, query.getCallTimeEnd());
        queryWrapper.orderBy("id", false);
        Page<SysOpenApiLog> page = openApiLogMapper.selectPageX(query, queryWrapper);
        List<OpenApiLogVO> list = converter.convert(page.getRecords(), OpenApiLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 调用日志详情
     */
    public OpenApiLogVO getOpenApiLog(Long id) {
        SysOpenApiLog openApiLog = openApiLogMapper.selectOneById(id);
        assertNotNull(openApiLog, SystemErrorCode.OPEN_API_LOG_NOT_FOUND, id);
        return converter.convert(openApiLog, OpenApiLogVO.class);
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
