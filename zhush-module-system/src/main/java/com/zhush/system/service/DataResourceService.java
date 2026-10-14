package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysDataField;
import com.zhush.system.entity.SysDataResource;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysDataFieldMapper;
import com.zhush.system.mapper.SysDataResourceMapper;
import com.zhush.system.param.DataResourceUpdateParam;
import com.zhush.system.query.DataResourceQuery;
import com.zhush.system.security.AdminDataScopeProvider;
import com.zhush.system.vo.DataFieldVO;
import com.zhush.system.vo.DataResourceVO;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 数据资源管理服务（元数据同步、资源查询、受控字段启停、资源编辑）
 *
 * @author zhush
 */
@Service
public class DataResourceService {

    private final SysDataResourceMapper dataResourceMapper;
    private final SysDataFieldMapper dataFieldMapper;
    private final DataResourceMetadataSyncer metadataSyncer;
    private final AdminDataScopeProvider dataScopeProvider;

    public DataResourceService(SysDataResourceMapper dataResourceMapper,
                               SysDataFieldMapper dataFieldMapper,
                               DataResourceMetadataSyncer metadataSyncer,
                               AdminDataScopeProvider dataScopeProvider) {
        this.dataResourceMapper = dataResourceMapper;
        this.dataFieldMapper = dataFieldMapper;
        this.metadataSyncer = metadataSyncer;
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 触发元数据同步（扫描代码注解登记/刷新资源与字段），随后失效数据范围缓存
     */
    public DataResourceMetadataSyncer.SyncResult sync() {
        DataResourceMetadataSyncer.SyncResult result = metadataSyncer.sync();
        dataScopeProvider.invalidateAll();
        return result;
    }

    /**
     * 资源分页
     */
    public PageResult<DataResourceVO> page(DataResourceQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysDataResource::getName, query.getName())
                .like(SysDataResource::getCode, query.getCode())
                .eq(SysDataResource::getModule, query.getModule())
                .eq(SysDataResource::getStatus, query.getStatus());
        queryWrapper.orderBy("code", true);
        Page<SysDataResource> page = dataResourceMapper.selectPageX(query, queryWrapper);
        List<DataResourceVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 全部启用资源（角色数据权限配置页 Tab 数据源）
     */
    public List<DataResourceVO> listEnabled() {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysDataResource::getStatus, 1);
        queryWrapper.orderBy("code", true);
        return dataResourceMapper.selectListByQuery(queryWrapper).stream().map(this::toVO).toList();
    }

    /**
     * 资源详情
     */
    public DataResourceVO get(Long id) {
        SysDataResource resource = dataResourceMapper.selectOneById(id);
        assertNotNull(resource, SystemErrorCode.DATA_RESOURCE_NOT_FOUND, id);
        return toVO(resource);
    }

    /**
     * 资源受控字段列表
     */
    public List<DataFieldVO> listFields(Long resourceId) {
        return dataFieldMapper.selectListByResourceId(resourceId).stream().map(this::toFieldVO).toList();
    }

    /**
     * 编辑资源（名称、条件列白名单、行列开关、状态）
     */
    public void update(DataResourceUpdateParam param) {
        SysDataResource existing = dataResourceMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.DATA_RESOURCE_NOT_FOUND, param.getId());
        SysDataResource update = new SysDataResource();
        update.setId(param.getId());
        update.setName(param.getName());
        update.setFilterColumns(param.getFilterColumns());
        update.setRowEnabled(param.getRowEnabled());
        update.setFieldEnabled(param.getFieldEnabled());
        update.setStatus(param.getStatus());
        dataResourceMapper.update(update);
        // 资源管控开关/白名单变化影响解析，失效缓存
        dataScopeProvider.invalidateAll();
    }

    /**
     * 启停单个受控字段
     */
    public void toggleField(Long fieldId, Integer status) {
        SysDataField existing = dataFieldMapper.selectOneById(fieldId);
        assertNotNull(existing, SystemErrorCode.DATA_FIELD_NOT_FOUND, fieldId);
        SysDataField update = new SysDataField();
        update.setId(fieldId);
        update.setStatus(status);
        dataFieldMapper.update(update);
    }

    // ================ 内部方法 ================

    private DataResourceVO toVO(SysDataResource resource) {
        DataResourceVO vo = new DataResourceVO();
        vo.setId(resource.getId());
        vo.setCode(resource.getCode());
        vo.setName(resource.getName());
        vo.setModule(resource.getModule());
        vo.setTableName(resource.getTableName());
        vo.setDeptColumn(resource.getDeptColumn());
        vo.setUserColumn(resource.getUserColumn());
        vo.setFilterColumns(resource.getFilterColumns());
        vo.setRowEnabled(resource.getRowEnabled());
        vo.setFieldEnabled(resource.getFieldEnabled());
        vo.setStatus(resource.getStatus());
        vo.setCreateTime(resource.getCreateTime());
        return vo;
    }

    private DataFieldVO toFieldVO(SysDataField field) {
        DataFieldVO vo = new DataFieldVO();
        vo.setId(field.getId());
        vo.setResourceId(field.getResourceId());
        vo.setFieldPath(field.getFieldPath());
        vo.setLabel(field.getLabel());
        vo.setPermissionCode(field.getPermissionCode());
        vo.setMenuId(field.getMenuId());
        vo.setDefaultStrategy(field.getDefaultStrategy());
        vo.setMaskValue(field.getMaskValue());
        vo.setStatus(field.getStatus());
        return vo;
    }
}
