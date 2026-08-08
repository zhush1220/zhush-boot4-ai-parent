package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionApiCallLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionApiCallLogMapper;
import com.zhush.union.param.UnionApiCallLogParam;
import com.zhush.union.query.UnionApiCallLogQuery;
import com.zhush.union.vo.UnionApiCallLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionApiCallLogService
 * @Description 平台API调用日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionApiCallLogService {

    private final UnionApiCallLogMapper apiCallLogMapper;
    private final Converter converter;

    public UnionApiCallLogService(UnionApiCallLogMapper apiCallLogMapper, Converter converter) {
        this.apiCallLogMapper = apiCallLogMapper;
        this.converter = converter;
    }

    /**
     * 新增平台API调用日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createApiCallLog(UnionApiCallLogParam param) {
        UnionApiCallLog entity = converter.convert(param, UnionApiCallLog.class);
        apiCallLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改平台API调用日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateApiCallLog(UnionApiCallLogParam param) {
        UnionApiCallLog existing = apiCallLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.API_CALL_LOG_NOT_FOUND, param.getId());
        UnionApiCallLog update = converter.convert(param, UnionApiCallLog.class);
        apiCallLogMapper.update(update);
    }

    /**
     * 删除平台API调用日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiCallLog(Long id) {
        UnionApiCallLog existing = apiCallLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.API_CALL_LOG_NOT_FOUND, id);
        apiCallLogMapper.deleteById(id);
    }

    /**
     * 平台API调用日志详情
     */
    public UnionApiCallLogVO getApiCallLog(Long id) {
        UnionApiCallLog entity = apiCallLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.API_CALL_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionApiCallLogVO.class);
    }

    /**
     * 平台API调用日志分页
     */
    public PageResult<UnionApiCallLogVO> getApiCallLogPage(UnionApiCallLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionApiCallLog::getPlatform, query.getPlatform())
                .like(UnionApiCallLog::getApiName, query.getApiName())
                .like(UnionApiCallLog::getRequestParams, query.getRequestParams())
                .like(UnionApiCallLog::getResponseCode, query.getResponseCode())
                .like(UnionApiCallLog::getResponseMsg, query.getResponseMsg())
                .eq(UnionApiCallLog::getDuration, query.getDuration())
        ;
        Page<UnionApiCallLog> page = apiCallLogMapper.selectPageX(query, queryWrapper);
        List<UnionApiCallLogVO> list = converter.convert(page.getRecords(), UnionApiCallLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
