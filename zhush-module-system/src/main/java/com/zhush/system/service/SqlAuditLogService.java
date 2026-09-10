package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysSqlAuditLog;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysSqlAuditLogMapper;
import com.zhush.system.query.SqlAuditLogQuery;
import com.zhush.system.vo.SqlAuditLogVO;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * SQL 审计日志服务
 *
 * <p>写路径由框架层 {@code SqlAuditMessageReporter}（原生 JdbcTemplate，绕开 MyBatis-Flex 拦截器）
 * 直接落库，本服务不负责写入，仅提供管理端的分页查询、详情与逻辑删除能力。
 *
 * @author zhush
 */
@Slf4j
@Service
public class SqlAuditLogService {

    private final SysSqlAuditLogMapper sqlAuditLogMapper;
    private final Converter converter;

    public SqlAuditLogService(SysSqlAuditLogMapper sqlAuditLogMapper, Converter converter) {
        this.sqlAuditLogMapper = sqlAuditLogMapper;
        this.converter = converter;
    }

    /**
     * SQL 审计日志分页
     */
    public PageResult<SqlAuditLogVO> getSqlAuditPage(SqlAuditLogQuery query) {
        QueryWrapperX queryWrapper = buildQueryWrapper(query);
        queryWrapper.orderBy("id", false);
        Page<SysSqlAuditLog> page = sqlAuditLogMapper.selectPageX(query, queryWrapper);
        List<SqlAuditLogVO> list = converter.convert(page.getRecords(), SqlAuditLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * SQL 审计日志详情
     */
    public SqlAuditLogVO getSqlAudit(Long id) {
        SysSqlAuditLog sqlAuditLog = sqlAuditLogMapper.selectOneById(id);
        assertNotNull(sqlAuditLog, SystemErrorCode.SQL_AUDIT_LOG_NOT_FOUND, id);
        return converter.convert(sqlAuditLog, SqlAuditLogVO.class);
    }

    /**
     * 删除 SQL 审计日志（逻辑删除）
     */
    public void deleteSqlAudit(Long id) {
        SysSqlAuditLog sqlAuditLog = sqlAuditLogMapper.selectOneById(id);
        assertNotNull(sqlAuditLog, SystemErrorCode.SQL_AUDIT_LOG_NOT_FOUND, id);
        sqlAuditLogMapper.deleteById(id);
    }

    /**
     * 构建 SQL 审计日志查询条件
     */
    private QueryWrapperX buildQueryWrapper(SqlAuditLogQuery query) {
        return QueryWrapperX.create()
                .eq(SysSqlAuditLog::getTraceId, query.getTraceId())
                .eq(SysSqlAuditLog::getPlatform, query.getPlatform())
                .like(SysSqlAuditLog::getUser, query.getUser())
                .like(SysSqlAuditLog::getStmtId, query.getStmtId())
                .eq(SysSqlAuditLog::getDsName, query.getDsName())
                .ge(SysSqlAuditLog::getQueryTime, query.getStartTime())
                .le(SysSqlAuditLog::getQueryTime, query.getEndTime());
    }

}
