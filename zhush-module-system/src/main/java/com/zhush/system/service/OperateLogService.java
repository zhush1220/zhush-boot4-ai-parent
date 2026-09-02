package com.zhush.system.service;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.PageResult;
import com.zhush.excel.core.ExcelUtils;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import com.zhush.system.entity.SysOperateLog;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysOperateLogMapper;
import com.zhush.system.query.OperateLogQuery;
import com.zhush.system.vo.OperateLogVO;
import com.zhush.web.operatelog.core.OperateLogHandler;
import com.zhush.web.operatelog.core.OperateLogRecord;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.github.linpeilie.Converter;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 操作日志服务
 *
 * <p>实现框架层 {@link OperateLogHandler}：切面采集完成后，在请求线程内补全操作人信息
 * （此时 Sa-Token 上下文仍可用），再交给独立线程池异步写入 {@code sys_operate_log} 表，
 * 不阻塞业务主流程；同时提供管理端的分页查询与详情能力。
 *
 * @author zhush
 */
@Slf4j
@Service
public class OperateLogService implements OperateLogHandler {

    /**
     * 导出日志最大行数（防止一次性导出过大数据量）
     */
    private static final long MAX_EXPORT_ROWS = 10000;

    /**
     * 时间格式化（用于导出 Excel）
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 异步落库线程池：单线程顺序写入，队列打满时丢弃并告警（日志不能拖垮业务）
     */
    private final ExecutorService logExecutor = new ThreadPoolExecutor(
            1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8192),
            runnable -> {
                Thread thread = new Thread(runnable, "operate-log-writer");
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> log.warn("[OperateLog] 日志队列已满，丢弃一条操作日志"));

    private final SysOperateLogMapper operateLogMapper;
    private final Converter converter;

    public OperateLogService(SysOperateLogMapper operateLogMapper, Converter converter) {
        this.operateLogMapper = operateLogMapper;
        this.converter = converter;
    }

    /**
     * 处理切面采集的操作日志：补全操作人后异步落库
     */
    @Override
    public void handle(OperateLogRecord record) {
        // 请求线程内补全操作人信息：管理端优先，其次会员端
        fillOperator(record);

        SysOperateLog operateLog = BeanUtil.copyProperties(record, SysOperateLog.class);
        logExecutor.execute(() -> {
            try {
                // insertSelective：忽略 null 字段，由 DB 列默认值兜底（NOT NULL DEFAULT '' 列），
                // 避免 MyBatis-Flex 显式插入 null 触发 SQLIntegrityConstraintViolationException
                operateLogMapper.insertSelective(operateLog);
            } catch (Exception e) {
                log.error("[OperateLog] 操作日志落库失败, log={}", operateLog, e);
            }
        });
    }

    /**
     * 操作日志分页
     */
    public PageResult<OperateLogVO> getOperateLogPage(OperateLogQuery query) {
        QueryWrapperX queryWrapper = buildQueryWrapper(query);
        queryWrapper.orderBy("id", false);
        Page<SysOperateLog> page = operateLogMapper.selectPageX(query, queryWrapper);
        List<OperateLogVO> list = converter.convert(page.getRecords(), OperateLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 操作日志详情
     */
    public OperateLogVO getOperateLog(Long id) {
        SysOperateLog operateLog = operateLogMapper.selectOneById(id);
        assertNotNull(operateLog, SystemErrorCode.OPERATE_LOG_NOT_FOUND, id);
        return converter.convert(operateLog, OperateLogVO.class);
    }

    /**
     * 导出操作日志（当前查询条件下的数据，最多导出 {@link #MAX_EXPORT_ROWS} 条）
     *
     * <p>动态表头方式写出到响应流，操作类型/用户类型/时间已转为可读文案。
     */
    public void exportOperateLog(OperateLogQuery query, HttpServletResponse response) {
        QueryWrapperX queryWrapper = buildQueryWrapper(query);
        queryWrapper.orderBy("id", false);
        queryWrapper.limit(MAX_EXPORT_ROWS);
        List<SysOperateLog> records = operateLogMapper.selectListByQuery(queryWrapper);

        List<List<String>> head = List.of(
                List.of("日志ID"), List.of("链路追踪ID"), List.of("操作人ID"), List.of("操作人类型"),
                List.of("所属模块"), List.of("操作名称"), List.of("操作类型"), List.of("请求方式"),
                List.of("请求地址"), List.of("操作人IP"), List.of("操作时间"), List.of("耗时(ms)"),
                List.of("结果码"), List.of("结果信息"));
        List<List<Object>> data = records.stream().map(log -> List.<Object>of(
                log.getId(), log.getTraceId(), log.getUserId(), userTypeLabel(log.getUserType()),
                log.getModule(), log.getName(), operateTypeLabel(log.getType()), log.getRequestMethod(),
                log.getRequestUrl(), log.getUserIp(), formatTime(log.getStartTime()), log.getDuration(),
                log.getResultCode(), log.getResultMsg())).toList();
        ExcelUtils.export(response, "操作日志", "操作日志", head, data);
    }

    /**
     * 构建操作日志查询条件（分页与导出共用）
     */
    private QueryWrapperX buildQueryWrapper(OperateLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysOperateLog::getUserId, query.getUserId())
                .eq(SysOperateLog::getUserType, query.getUserType())
                .like(SysOperateLog::getModule, query.getModule())
                .like(SysOperateLog::getName, query.getName())
                .eq(SysOperateLog::getType, query.getType())
                .like(SysOperateLog::getRequestUrl, query.getRequestUrl())
                .ge(SysOperateLog::getStartTime, query.getStartTimeBegin())
                .le(SysOperateLog::getStartTime, query.getStartTimeEnd());
        // 成功/失败过滤：resultCode = 0 表示成功
        if (Boolean.TRUE.equals(query.getSuccess())) {
            queryWrapper.eq(SysOperateLog::getResultCode, GlobalErrorCode.SUCCESS.getCode());
        } else if (Boolean.FALSE.equals(query.getSuccess())) {
            queryWrapper.ne(SysOperateLog::getResultCode, GlobalErrorCode.SUCCESS.getCode());
        }
        return queryWrapper;
    }

    /**
     * 操作类型中文文案
     */
    private static String operateTypeLabel(Integer type) {
        if (type == null) {
            return "";
        }
        return Arrays.stream(OperateTypeEnum.values())
                .filter(e -> e.getType() == type)
                .map(OperateTypeEnum::getDescription)
                .findFirst()
                .orElse(String.valueOf(type));
    }

    /**
     * 用户类型中文文案
     */
    private static String userTypeLabel(String userType) {
        if (userType == null) {
            return "";
        }
        return "admin".equals(userType) ? "管理端"
                : "member".equals(userType) ? "会员端"
                : userType;
    }

    /**
     * 时间格式化为字符串，避免 Excel 序列化 LocalDateTime 异常
     */
    private static String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    // ================ 内部方法 ================

    private void fillOperator(OperateLogRecord record) {
        if (record.getUserId() != null) {
            return;
        }
        Long adminId = StpAdminUtil.getLoginIdDefaultNull();
        if (adminId != null) {
            record.setUserId(adminId);
            record.setUserType(LoginTypeConstants.ADMIN);
            return;
        }
        Long memberId = StpMemberUtil.getLoginIdDefaultNull();
        if (memberId != null) {
            record.setUserId(memberId);
            record.setUserType(LoginTypeConstants.MEMBER);
        }
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
