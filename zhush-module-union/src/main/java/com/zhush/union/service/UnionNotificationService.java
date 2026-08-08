package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionNotification;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionNotificationMapper;
import com.zhush.union.param.UnionNotificationParam;
import com.zhush.union.query.UnionNotificationQuery;
import com.zhush.union.vo.UnionNotificationVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionNotificationService
 * @Description 消息通知表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionNotificationService {

    private final UnionNotificationMapper notificationMapper;
    private final Converter converter;

    public UnionNotificationService(UnionNotificationMapper notificationMapper, Converter converter) {
        this.notificationMapper = notificationMapper;
        this.converter = converter;
    }

    /**
     * 新增消息通知
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createNotification(UnionNotificationParam param) {
        UnionNotification entity = converter.convert(param, UnionNotification.class);
        notificationMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改消息通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNotification(UnionNotificationParam param) {
        UnionNotification existing = notificationMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.NOTIFICATION_NOT_FOUND, param.getId());
        UnionNotification update = converter.convert(param, UnionNotification.class);
        notificationMapper.update(update);
    }

    /**
     * 删除消息通知（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id) {
        UnionNotification existing = notificationMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.NOTIFICATION_NOT_FOUND, id);
        notificationMapper.deleteById(id);
    }

    /**
     * 消息通知详情
     */
    public UnionNotificationVO getNotification(Long id) {
        UnionNotification entity = notificationMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.NOTIFICATION_NOT_FOUND, id);
        return converter.convert(entity, UnionNotificationVO.class);
    }

    /**
     * 消息通知分页
     */
    public PageResult<UnionNotificationVO> getNotificationPage(UnionNotificationQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionNotification::getMemberId, query.getMemberId())
                .eq(UnionNotification::getNotifyType, query.getNotifyType())
                .like(UnionNotification::getTitle, query.getTitle())
                .like(UnionNotification::getContent, query.getContent())
                .like(UnionNotification::getLinkUrl, query.getLinkUrl())
                .like(UnionNotification::getExtraData, query.getExtraData())
                .eq(UnionNotification::getStatus, query.getStatus())
        ;
        Page<UnionNotification> page = notificationMapper.selectPageX(query, queryWrapper);
        List<UnionNotificationVO> list = converter.convert(page.getRecords(), UnionNotificationVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
