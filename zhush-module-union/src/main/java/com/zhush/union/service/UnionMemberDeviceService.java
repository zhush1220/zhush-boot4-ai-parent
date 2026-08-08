package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionMemberDevice;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionMemberDeviceMapper;
import com.zhush.union.param.UnionMemberDeviceParam;
import com.zhush.union.query.UnionMemberDeviceQuery;
import com.zhush.union.vo.UnionMemberDeviceVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionMemberDeviceService
 * @Description 推广者设备信息表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionMemberDeviceService {

    private final UnionMemberDeviceMapper memberDeviceMapper;
    private final Converter converter;

    public UnionMemberDeviceService(UnionMemberDeviceMapper memberDeviceMapper, Converter converter) {
        this.memberDeviceMapper = memberDeviceMapper;
        this.converter = converter;
    }

    /**
     * 新增推广者设备信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberDevice(UnionMemberDeviceParam param) {
        UnionMemberDevice entity = converter.convert(param, UnionMemberDevice.class);
        memberDeviceMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广者设备信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberDevice(UnionMemberDeviceParam param) {
        UnionMemberDevice existing = memberDeviceMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.MEMBER_DEVICE_NOT_FOUND, param.getId());
        UnionMemberDevice update = converter.convert(param, UnionMemberDevice.class);
        memberDeviceMapper.update(update);
    }

    /**
     * 删除推广者设备信息（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemberDevice(Long id) {
        UnionMemberDevice existing = memberDeviceMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.MEMBER_DEVICE_NOT_FOUND, id);
        memberDeviceMapper.deleteById(id);
    }

    /**
     * 推广者设备信息详情
     */
    public UnionMemberDeviceVO getMemberDevice(Long id) {
        UnionMemberDevice entity = memberDeviceMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.MEMBER_DEVICE_NOT_FOUND, id);
        return converter.convert(entity, UnionMemberDeviceVO.class);
    }

    /**
     * 推广者设备信息分页
     */
    public PageResult<UnionMemberDeviceVO> getMemberDevicePage(UnionMemberDeviceQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionMemberDevice::getMemberId, query.getMemberId())
                .like(UnionMemberDevice::getDeviceId, query.getDeviceId())
                .like(UnionMemberDevice::getDeviceType, query.getDeviceType())
                .like(UnionMemberDevice::getDeviceModel, query.getDeviceModel())
                .like(UnionMemberDevice::getOsVersion, query.getOsVersion())
                .like(UnionMemberDevice::getAppVersion, query.getAppVersion())
                .like(UnionMemberDevice::getLastLoginIp, query.getLastLoginIp())
                .eq(UnionMemberDevice::getLoginCount, query.getLoginCount())
                .eq(UnionMemberDevice::getStatus, query.getStatus())
        ;
        Page<UnionMemberDevice> page = memberDeviceMapper.selectPageX(query, queryWrapper);
        List<UnionMemberDeviceVO> list = converter.convert(page.getRecords(), UnionMemberDeviceVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
