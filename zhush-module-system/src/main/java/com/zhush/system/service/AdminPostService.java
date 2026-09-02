package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysPost;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysPostMapper;
import com.zhush.system.param.AdminPostParam;
import com.zhush.system.query.AdminPostQuery;
import com.zhush.system.vo.AdminPostVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 岗位管理服务（CRUD + 分页）
 *
 * @author zhush
 */
@Service
public class AdminPostService {

    private static final int STATUS_ENABLED = 1;
    private static final int DEFAULT_SORT = 0;

    private final SysPostMapper postMapper;
    private final Converter converter;

    public AdminPostService(SysPostMapper postMapper, Converter converter) {
        this.postMapper = postMapper;
        this.converter = converter;
    }

    /**
     * 新增岗位
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(AdminPostParam param) {
        validatePostCodeUnique(null, param.getPostCode());
        SysPost post = converter.convert(param, SysPost.class);
        if (post.getStatus() == null) {
            post.setStatus(STATUS_ENABLED);
        }
        if (post.getSort() == null) {
            post.setSort(DEFAULT_SORT);
        }
        postMapper.insert(post);
        return post.getId();
    }

    /**
     * 修改岗位
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(AdminPostParam param) {
        SysPost existing = postMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.POST_NOT_FOUND, param.getId());
        validatePostCodeUnique(param.getId(), param.getPostCode());
        postMapper.update(converter.convert(param, SysPost.class));
    }

    /**
     * 删除岗位（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        SysPost existing = postMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.POST_NOT_FOUND, id);
        postMapper.deleteById(id);
    }

    /**
     * 岗位详情
     */
    public AdminPostVO getPost(Long id) {
        SysPost post = postMapper.selectOneById(id);
        assertNotNull(post, SystemErrorCode.POST_NOT_FOUND, id);
        return converter.convert(post, AdminPostVO.class);
    }

    /**
     * 岗位分页
     */
    public PageResult<AdminPostVO> getPostPage(AdminPostQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysPost::getPostCode, query.getPostCode())
                .like(SysPost::getPostName, query.getPostName())
                .eq(SysPost::getStatus, query.getStatus());
        queryWrapper.orderBy("sort", true);
        Page<SysPost> page = postMapper.selectPageX(query, queryWrapper);
        List<AdminPostVO> list = converter.convert(page.getRecords(), AdminPostVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 内部方法 ================

    private void validatePostCodeUnique(Long id, String postCode) {
        if (StrUtil.isBlank(postCode)) {
            return;
        }
        SysPost existing = postMapper.selectByPostCode(postCode);
        if (existing == null) {
            return;
        }
        assertTrue(existing.getId().equals(id), SystemErrorCode.POST_CODE_DUPLICATE, postCode);
    }
}
