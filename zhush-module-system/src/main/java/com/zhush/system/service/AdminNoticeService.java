package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysNotice;
import com.zhush.system.entity.SysNoticeRecord;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysNoticeMapper;
import com.zhush.system.mapper.SysNoticeRecordMapper;
import com.zhush.system.param.AdminNoticeParam;
import com.zhush.system.query.AdminNoticeQuery;
import com.zhush.system.query.NoticeRecordQuery;
import com.zhush.system.vo.AdminNoticeVO;
import com.zhush.system.vo.NoticeRecordVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 通知公告服务（CRUD + 公告查看记录）
 *
 * @author zhush
 */
@Service
public class AdminNoticeService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeRecordMapper noticeRecordMapper;
    private final SysAdminUserMapper adminUserMapper;
    private final Converter converter;

    public AdminNoticeService(SysNoticeMapper noticeMapper,
                              SysNoticeRecordMapper noticeRecordMapper,
                              SysAdminUserMapper adminUserMapper,
                              Converter converter) {
        this.noticeMapper = noticeMapper;
        this.noticeRecordMapper = noticeRecordMapper;
        this.adminUserMapper = adminUserMapper;
        this.converter = converter;
    }

    /**
     * 新增公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(AdminNoticeParam param) {
        SysNotice notice = converter.convert(param, SysNotice.class);
        if (notice.getStatus() == null) {
            notice.setStatus(STATUS_DRAFT);
        }
        // 已发布则记录发布时间，否则清空
        notice.setPublishTime(notice.getStatus() == STATUS_PUBLISHED
                ? (param.getPublishTime() != null ? param.getPublishTime() : LocalDateTime.now())
                : null);
        noticeMapper.insert(notice);
        return notice.getId();
    }

    /**
     * 修改公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(AdminNoticeParam param) {
        SysNotice existing = noticeMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.NOTICE_NOT_FOUND, param.getId());

        SysNotice update = converter.convert(param, SysNotice.class);
        // 发布状态：由非发布切换为发布时写入发布时间；其余由参数决定
        if (update.getStatus() != null && update.getStatus() == STATUS_PUBLISHED && existing.getStatus() != STATUS_PUBLISHED) {
            update.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.update(update);
    }

    /**
     * 删除公告（逻辑删除，并清理其查看记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        SysNotice existing = noticeMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.NOTICE_NOT_FOUND, id);
        noticeMapper.deleteById(id);
        noticeRecordMapper.deleteByNoticeId(id);
    }

    /**
     * 批量删除公告（循环复用单条删除的校验与关联清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoticeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::deleteNotice);
    }

    /**
     * 公告详情
     */
    public AdminNoticeVO getNotice(Long id) {
        SysNotice notice = noticeMapper.selectOneById(id);
        assertNotNull(notice, SystemErrorCode.NOTICE_NOT_FOUND, id);
        return converter.convert(notice, AdminNoticeVO.class);
    }

    /**
     * 公告分页
     */
    public PageResult<AdminNoticeVO> getNoticePage(AdminNoticeQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysNotice::getTitle, query.getTitle())
                .eq(SysNotice::getType, query.getType())
                .eq(SysNotice::getStatus, query.getStatus())
                .ge(SysNotice::getCreateTime, query.getCreateTimeBegin())
                .le(SysNotice::getCreateTime, query.getCreateTimeEnd());
        queryWrapper.orderBy("create_time", false);
        Page<SysNotice> page = noticeMapper.selectPageX(query, queryWrapper);
        List<AdminNoticeVO> list = converter.convert(page.getRecords(), AdminNoticeVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 记录当前登录用户查看公告（每用户每公告一条，重复查看刷新 view_time）
     */
    @Transactional(rollbackFor = Exception.class)
    public void viewNotice(Long noticeId) {
        SysNotice notice = noticeMapper.selectOneById(noticeId);
        assertNotNull(notice, SystemErrorCode.NOTICE_NOT_FOUND, noticeId);
        // 仅已发布的公告记录查看，草稿/撤回不记录
        if (notice.getStatus() == null || notice.getStatus() != STATUS_PUBLISHED) {
            return;
        }
        Long userId = StpAdminUtil.getLoginIdDefaultNull();
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SysNoticeRecord existing = noticeRecordMapper.selectByNoticeIdAndUserId(noticeId, userId);
        if (existing != null) {
            existing.setViewTime(now);
            noticeRecordMapper.update(existing);
            return;
        }
        SysNoticeRecord record = new SysNoticeRecord();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setUserName(resolveUserName(userId));
        record.setViewTime(now);
        record.setCreateTime(now);
        noticeRecordMapper.insert(record);
    }

    /**
     * 公告查看记录分页
     */
    public PageResult<NoticeRecordVO> getNoticeRecordPage(Long noticeId, NoticeRecordQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysNoticeRecord::getNoticeId, noticeId)
                .like(SysNoticeRecord::getUserName, query.getUserName());
        queryWrapper.orderBy("view_time", false);
        Page<SysNoticeRecord> page = noticeRecordMapper.selectPageX(query, queryWrapper);
        List<NoticeRecordVO> list = converter.convert(page.getRecords(), NoticeRecordVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 内部方法 ================

    private String resolveUserName(Long userId) {
        SysAdminUser user = adminUserMapper.selectOneById(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
