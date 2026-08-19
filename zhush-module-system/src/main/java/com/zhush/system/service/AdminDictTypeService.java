package com.zhush.system.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysDictType;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysDictDataMapper;
import com.zhush.system.mapper.SysDictTypeMapper;
import com.zhush.system.param.AdminDictTypeParam;
import com.zhush.system.query.AdminDictTypeQuery;
import com.zhush.system.vo.AdminDictTypeVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertFalse;
import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 字典类型管理服务
 *
 * @author zhush
 */
@Service
public class AdminDictTypeService {

    private static final int STATUS_ENABLED = 1;

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final Converter converter;

    public AdminDictTypeService(SysDictTypeMapper dictTypeMapper,
                                SysDictDataMapper dictDataMapper,
                                Converter converter) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
        this.converter = converter;
    }

    /**
     * 新增字典类型
     */
    public Long createDictType(AdminDictTypeParam param) {
        validateTypeUnique(null, param.getType());
        SysDictType dictType = converter.convert(param, SysDictType.class);
        if (dictType.getStatus() == null) {
            dictType.setStatus(STATUS_ENABLED);
        }
        dictTypeMapper.insert(dictType);
        return dictType.getId();
    }

    /**
     * 修改字典类型（编码不可修改：业务方按编码取数，修改会导致存量引用失效）
     */
    public void updateDictType(AdminDictTypeParam param) {
        SysDictType existing = dictTypeMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.DICT_TYPE_NOT_FOUND, param.getId());

        SysDictType update = converter.convert(param, SysDictType.class);
        update.setType(null);
        dictTypeMapper.update(update);
    }

    /**
     * 删除字典类型（逻辑删除；类型下仍有数据时禁止删除）
     */
    public void deleteDictType(Long id) {
        SysDictType existing = dictTypeMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.DICT_TYPE_NOT_FOUND, id);
        assertTrue(dictDataMapper.countByDictType(existing.getType()) == 0,
                SystemErrorCode.DICT_TYPE_HAS_DATA);
        dictTypeMapper.deleteById(id);
    }

    /**
     * 字典类型详情
     */
    public AdminDictTypeVO getDictType(Long id) {
        SysDictType dictType = dictTypeMapper.selectOneById(id);
        assertNotNull(dictType, SystemErrorCode.DICT_TYPE_NOT_FOUND, id);
        return converter.convert(dictType, AdminDictTypeVO.class);
    }

    /**
     * 字典类型分页
     */
    public PageResult<AdminDictTypeVO> getDictTypePage(AdminDictTypeQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysDictType::getName, query.getName())
                .like(SysDictType::getType, query.getType())
                .eq(SysDictType::getStatus, query.getStatus());
        Page<SysDictType> page = dictTypeMapper.selectPageX(query, queryWrapper);
        List<AdminDictTypeVO> list = converter.convert(page.getRecords(), AdminDictTypeVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 内部方法 ================

    private void validateTypeUnique(Long id, String type) {
        SysDictType dictType = dictTypeMapper.selectByType(type);
        if (dictType == null) {
            return;
        }
        assertFalse(id == null || !dictType.getId().equals(id),
                SystemErrorCode.DICT_TYPE_DUPLICATE, type);
    }
}
