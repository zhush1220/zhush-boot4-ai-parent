package com.zhush.system.service;

import com.zhush.common.exception.ServiceException;
import com.zhush.system.entity.SysDictData;
import com.zhush.system.entity.SysDictType;
import com.zhush.system.mapper.SysDictDataMapper;
import com.zhush.system.mapper.SysDictTypeMapper;
import com.zhush.system.param.AdminDictDataParam;
import com.zhush.system.param.AdminDictTypeParam;
import io.github.linpeilie.Converter;
import com.zhush.redis.core.RedisCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据字典服务测试：编码/键值唯一性、删除保护、类型不可修改、Redis 缓存失效
 */
@ExtendWith(MockitoExtension.class)
class AdminDictServiceTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;
    @Mock
    private SysDictDataMapper dictDataMapper;
    @Mock
    private Converter converter;
    @Mock
    private RedisCache redisCache;
    @InjectMocks
    private AdminDictTypeService dictTypeService;
    @InjectMocks
    private AdminDictDataService dictDataService;

    private SysDictType dictType(Long id, String type) {
        SysDictType dictType = new SysDictType();
        dictType.setId(id);
        dictType.setType(type);
        dictType.setStatus(1);
        return dictType;
    }

    private SysDictData dictData(Long id, String dictType, String value) {
        SysDictData data = new SysDictData();
        data.setId(id);
        data.setDictType(dictType);
        data.setValue(value);
        data.setStatus(1);
        return data;
    }

    private AdminDictTypeParam typeParam(Long id, String type) {
        AdminDictTypeParam param = new AdminDictTypeParam();
        param.setId(id);
        param.setType(type);
        return param;
    }

    private AdminDictDataParam dataParam(Long id, String dictType, String value) {
        AdminDictDataParam param = new AdminDictDataParam();
        param.setId(id);
        param.setDictType(dictType);
        param.setValue(value);
        return param;
    }

    // ================ 字典类型 ================

    @Test
    void createDictTypeRejectsDuplicateType() {
        when(dictTypeMapper.selectByType("common_status")).thenReturn(dictType(1L, "common_status"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dictTypeService.createDictType(typeParam(null, "common_status")));

        assertEquals(1001802, ex.getCode());
    }

    @Test
    void createDictTypeDefaultsStatusEnabled() {
        SysDictType converted = new SysDictType();
        doReturn(converted).when(converter)
                .convert(ArgumentMatchers.<Object>any(), eq(SysDictType.class));

        dictTypeService.createDictType(typeParam(null, "new_type"));

        // 未传状态时默认启用
        assertEquals(1, converted.getStatus());
        verify(dictTypeMapper).insert(converted);
    }

    @Test
    void updateDictTypeKeepsTypeImmutable() {
        when(dictTypeMapper.selectOneById(1L)).thenReturn(dictType(1L, "common_status"));
        SysDictType converted = new SysDictType();
        converted.setId(1L);
        converted.setType("hacked_type");
        doReturn(converted).when(converter)
                .convert(ArgumentMatchers.<Object>any(), eq(SysDictType.class));

        dictTypeService.updateDictType(typeParam(1L, "hacked_type"));

        // 编码被强制置空，不允许修改
        assertEquals(null, converted.getType());
        verify(dictTypeMapper).update(converted);
    }

    @Test
    void deleteDictTypeBlockedWhenDataExists() {
        when(dictTypeMapper.selectOneById(1L)).thenReturn(dictType(1L, "common_status"));
        when(dictDataMapper.countByDictType("common_status")).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dictTypeService.deleteDictType(1L));

        assertEquals(1001803, ex.getCode());
        verify(dictTypeMapper, never()).deleteById(any());
    }

    // ================ 字典数据 ================

    @Test
    void createDictDataRejectsWhenTypeMissing() {
        when(dictTypeMapper.selectByType("ghost")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dictDataService.createDictData(dataParam(null, "ghost", "1")));

        assertEquals(1001801, ex.getCode());
    }

    @Test
    void createDictDataRejectsDuplicateValue() {
        when(dictTypeMapper.selectByType("common_status")).thenReturn(dictType(1L, "common_status"));
        when(dictDataMapper.selectByDictTypeAndValue("common_status", "1"))
                .thenReturn(dictData(10L, "common_status", "1"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dictDataService.createDictData(dataParam(null, "common_status", "1")));

        assertEquals(1001805, ex.getCode());
    }

    @Test
    void createDictDataEvictsCache() {
        when(dictTypeMapper.selectByType("common_status")).thenReturn(dictType(1L, "common_status"));
        SysDictData converted = new SysDictData();
        doReturn(converted).when(converter)
                .convert(ArgumentMatchers.<Object>any(), eq(SysDictData.class));

        dictDataService.createDictData(dataParam(null, "common_status", "2"));

        verify(dictDataMapper).insert(converted);
        verify(redisCache).delete("zhush:dict:data:common_status");
    }

    @Test
    void deleteDictDataEvictsCache() {
        when(dictDataMapper.selectOneById(10L)).thenReturn(dictData(10L, "common_status", "1"));

        dictDataService.deleteDictData(10L);

        verify(dictDataMapper).deleteById(10L);
        verify(redisCache).delete("zhush:dict:data:common_status");
    }

    @Test
    void getDictDataListUsesCacheAside() {
        // 缓存命中：不查库
        when(redisCache.<List<com.zhush.system.vo.AdminDictDataVO>>get(
                eq("zhush:dict:data:common_status"), any(Duration.class), any()))
                .thenReturn(List.of(mock(com.zhush.system.vo.AdminDictDataVO.class)));

        List<com.zhush.system.vo.AdminDictDataVO> list = dictDataService.getDictDataList("common_status");

        assertEquals(1, list.size());
        verify(dictDataMapper, never()).selectEnabledListByDictType(any());
    }

    @Test
    void updateDictDataEvictsBothTypesOnTypeChange() {
        when(dictDataMapper.selectOneById(10L)).thenReturn(dictData(10L, "old_type", "1"));
        when(dictTypeMapper.selectByType("new_type")).thenReturn(dictType(2L, "new_type"));
        SysDictData converted = new SysDictData();
        doReturn(converted).when(converter)
                .convert(ArgumentMatchers.<Object>any(), eq(SysDictData.class));

        dictDataService.updateDictData(dataParam(10L, "new_type", "1"));

        verify(redisCache).delete("zhush:dict:data:old_type");
        verify(redisCache).delete("zhush:dict:data:new_type");
    }
}
