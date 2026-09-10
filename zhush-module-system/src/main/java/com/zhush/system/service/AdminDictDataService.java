package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.redis.core.RedisCache;
import com.zhush.system.entity.SysDictData;
import com.zhush.system.entity.SysDictType;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysDictDataMapper;
import com.zhush.system.mapper.SysDictTypeMapper;
import com.zhush.system.param.AdminDictDataParam;
import com.zhush.system.query.AdminDictDataQuery;
import com.zhush.system.vo.AdminDictDataVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertFalse;
import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 字典数据管理服务
 *
 * <p>写操作同步维护 Redis 缓存；读接口 {@link #getDictDataList(String)} 走
 * Cache-Aside（缓存 1 天，变更即失效），供全项目按字典类型编码取数。
 *
 * @author zhush
 */
@Service
public class AdminDictDataService {

    private static final int STATUS_ENABLED = 1;

    /**
     * 字典数据缓存 key 前缀：zhush:dict:data:{dictType}
     */
    private static final String CACHE_KEY_PREFIX = "zhush:dict:data:";

    /**
     * 缓存有效期（变更时主动失效，此处仅为兜底）
     */
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final SysDictDataMapper dictDataMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final Converter converter;
    private final RedisCache redisCache;

    public AdminDictDataService(SysDictDataMapper dictDataMapper,
                                SysDictTypeMapper dictTypeMapper,
                                Converter converter,
                                RedisCache redisCache) {
        this.dictDataMapper = dictDataMapper;
        this.dictTypeMapper = dictTypeMapper;
        this.converter = converter;
        this.redisCache = redisCache;
    }

    /**
     * 新增字典数据
     */
    public Long createDictData(AdminDictDataParam param) {
        validateDictTypeExists(param.getDictType());
        validateValueUnique(null, param.getDictType(), param.getValue());
        SysDictData dictData = converter.convert(param, SysDictData.class);
        if (dictData.getStatus() == null) {
            dictData.setStatus(STATUS_ENABLED);
        }
        dictDataMapper.insert(dictData);
        evictCache(param.getDictType());
        return dictData.getId();
    }

    /**
     * 修改字典数据
     */
    public void updateDictData(AdminDictDataParam param) {
        SysDictData existing = dictDataMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.DICT_DATA_NOT_FOUND, param.getId());
        validateDictTypeExists(param.getDictType());
        validateValueUnique(param.getId(), param.getDictType(), param.getValue());

        SysDictData update = converter.convert(param, SysDictData.class);
        dictDataMapper.update(update);
        // 类型变更时新旧缓存均需失效
        evictCache(existing.getDictType());
        if (!existing.getDictType().equals(param.getDictType())) {
            evictCache(param.getDictType());
        }
    }

    /**
     * 删除字典数据（逻辑删除）
     */
    public void deleteDictData(Long id) {
        SysDictData existing = dictDataMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.DICT_DATA_NOT_FOUND, id);
        dictDataMapper.deleteById(id);
        evictCache(existing.getDictType());
    }

    /**
     * 批量删除字典数据（循环复用单条删除的校验与缓存清理）
     */
    public void deleteDictDataBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::deleteDictData);
    }

    /**
     * 字典数据详情
     */
    public AdminDictDataVO getDictData(Long id) {
        SysDictData dictData = dictDataMapper.selectOneById(id);
        assertNotNull(dictData, SystemErrorCode.DICT_DATA_NOT_FOUND, id);
        return converter.convert(dictData, AdminDictDataVO.class);
    }

    /**
     * 字典数据分页
     */
    public PageResult<AdminDictDataVO> getDictDataPage(AdminDictDataQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysDictData::getDictType, query.getDictType())
                .like(SysDictData::getLabel, query.getLabel())
                .eq(SysDictData::getStatus, query.getStatus());
        queryWrapper.orderBy("sort", true);
        Page<SysDictData> page = dictDataMapper.selectPageX(query, queryWrapper);
        List<AdminDictDataVO> list = converter.convert(page.getRecords(), AdminDictDataVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 按字典类型编码查询启用中的字典数据（公共取数入口，走 Redis 缓存）
     */
    public List<AdminDictDataVO> getDictDataList(String dictType) {
        return redisCache.get(CACHE_KEY_PREFIX + dictType, CACHE_TTL, () ->
                converter.convert(dictDataMapper.selectEnabledListByDictType(dictType), AdminDictDataVO.class));
    }

    // ================ 内部方法 ================

    private void validateDictTypeExists(String dictType) {
        SysDictType type = dictTypeMapper.selectByType(dictType);
        assertNotNull(type, SystemErrorCode.DICT_TYPE_NOT_FOUND, dictType);
    }

    private void validateValueUnique(Long id, String dictType, String value) {
        SysDictData dictData = dictDataMapper.selectByDictTypeAndValue(dictType, value);
        if (dictData == null) {
            return;
        }
        assertFalse(id == null || !dictData.getId().equals(id),
                SystemErrorCode.DICT_DATA_DUPLICATE, value);
    }

    private void evictCache(String dictType) {
        redisCache.delete(CACHE_KEY_PREFIX + dictType);
    }
}
