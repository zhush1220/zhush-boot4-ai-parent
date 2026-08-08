package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionLinkRequestLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionLinkRequestLogMapper;
import com.zhush.union.param.UnionLinkRequestLogParam;
import com.zhush.union.query.UnionLinkRequestLogQuery;
import com.zhush.union.vo.UnionLinkRequestLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionLinkRequestLogService
 * @Description 转链请求日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionLinkRequestLogService {

    private final UnionLinkRequestLogMapper linkRequestLogMapper;
    private final Converter converter;

    public UnionLinkRequestLogService(UnionLinkRequestLogMapper linkRequestLogMapper, Converter converter) {
        this.linkRequestLogMapper = linkRequestLogMapper;
        this.converter = converter;
    }

    /**
     * 新增转链请求日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createLinkRequestLog(UnionLinkRequestLogParam param) {
        UnionLinkRequestLog entity = converter.convert(param, UnionLinkRequestLog.class);
        linkRequestLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改转链请求日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLinkRequestLog(UnionLinkRequestLogParam param) {
        UnionLinkRequestLog existing = linkRequestLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.LINK_REQUEST_LOG_NOT_FOUND, param.getId());
        UnionLinkRequestLog update = converter.convert(param, UnionLinkRequestLog.class);
        linkRequestLogMapper.update(update);
    }

    /**
     * 删除转链请求日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLinkRequestLog(Long id) {
        UnionLinkRequestLog existing = linkRequestLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.LINK_REQUEST_LOG_NOT_FOUND, id);
        linkRequestLogMapper.deleteById(id);
    }

    /**
     * 转链请求日志详情
     */
    public UnionLinkRequestLogVO getLinkRequestLog(Long id) {
        UnionLinkRequestLog entity = linkRequestLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.LINK_REQUEST_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionLinkRequestLogVO.class);
    }

    /**
     * 转链请求日志分页
     */
    public PageResult<UnionLinkRequestLogVO> getLinkRequestLogPage(UnionLinkRequestLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionLinkRequestLog::getMemberId, query.getMemberId())
                .eq(UnionLinkRequestLog::getPlatform, query.getPlatform())
                .eq(UnionLinkRequestLog::getSourceType, query.getSourceType())
                .like(UnionLinkRequestLog::getSourceInput, query.getSourceInput())
                .like(UnionLinkRequestLog::getOuterProductId, query.getOuterProductId())
                .eq(UnionLinkRequestLog::getProductId, query.getProductId())
                .eq(UnionLinkRequestLog::getLinkType, query.getLinkType())
                .like(UnionLinkRequestLog::getRequestIp, query.getRequestIp())
                .like(UnionLinkRequestLog::getUserAgent, query.getUserAgent())
                .eq(UnionLinkRequestLog::getResultStatus, query.getResultStatus())
                .eq(UnionLinkRequestLog::getPromotionLinkId, query.getPromotionLinkId())
                .like(UnionLinkRequestLog::getPromotionUrl, query.getPromotionUrl())
                .like(UnionLinkRequestLog::getPromotionCode, query.getPromotionCode())
                .like(UnionLinkRequestLog::getShortUrl, query.getShortUrl())
                .like(UnionLinkRequestLog::getErrorCode, query.getErrorCode())
                .like(UnionLinkRequestLog::getErrorMsg, query.getErrorMsg())
                .eq(UnionLinkRequestLog::getApiCallDuration, query.getApiCallDuration())
                .eq(UnionLinkRequestLog::getTotalDuration, query.getTotalDuration())
        ;
        Page<UnionLinkRequestLog> page = linkRequestLogMapper.selectPageX(query, queryWrapper);
        List<UnionLinkRequestLogVO> list = converter.convert(page.getRecords(), UnionLinkRequestLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
