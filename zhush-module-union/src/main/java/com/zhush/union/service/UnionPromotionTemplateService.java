package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionPromotionTemplate;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionPromotionTemplateMapper;
import com.zhush.union.param.UnionPromotionTemplateParam;
import com.zhush.union.query.UnionPromotionTemplateQuery;
import com.zhush.union.vo.UnionPromotionTemplateVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionPromotionTemplateService
 * @Description 推广文案模板表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionPromotionTemplateService {

    private final UnionPromotionTemplateMapper promotionTemplateMapper;
    private final Converter converter;

    public UnionPromotionTemplateService(UnionPromotionTemplateMapper promotionTemplateMapper, Converter converter) {
        this.promotionTemplateMapper = promotionTemplateMapper;
        this.converter = converter;
    }

    /**
     * 新增推广文案模板
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPromotionTemplate(UnionPromotionTemplateParam param) {
        UnionPromotionTemplate entity = converter.convert(param, UnionPromotionTemplate.class);
        promotionTemplateMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广文案模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePromotionTemplate(UnionPromotionTemplateParam param) {
        UnionPromotionTemplate existing = promotionTemplateMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PROMOTION_TEMPLATE_NOT_FOUND, param.getId());
        UnionPromotionTemplate update = converter.convert(param, UnionPromotionTemplate.class);
        promotionTemplateMapper.update(update);
    }

    /**
     * 删除推广文案模板（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotionTemplate(Long id) {
        UnionPromotionTemplate existing = promotionTemplateMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PROMOTION_TEMPLATE_NOT_FOUND, id);
        promotionTemplateMapper.deleteById(id);
    }

    /**
     * 推广文案模板详情
     */
    public UnionPromotionTemplateVO getPromotionTemplate(Long id) {
        UnionPromotionTemplate entity = promotionTemplateMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PROMOTION_TEMPLATE_NOT_FOUND, id);
        return converter.convert(entity, UnionPromotionTemplateVO.class);
    }

    /**
     * 推广文案模板分页
     */
    public PageResult<UnionPromotionTemplateVO> getPromotionTemplatePage(UnionPromotionTemplateQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionPromotionTemplate::getTemplateName, query.getTemplateName())
                .eq(UnionPromotionTemplate::getPlatform, query.getPlatform())
                .eq(UnionPromotionTemplate::getCategoryId, query.getCategoryId())
                .eq(UnionPromotionTemplate::getTemplateType, query.getTemplateType())
                .like(UnionPromotionTemplate::getTitleTemplate, query.getTitleTemplate())
                .like(UnionPromotionTemplate::getDescTemplate, query.getDescTemplate())
                .like(UnionPromotionTemplate::getImageUrl, query.getImageUrl())
                .like(UnionPromotionTemplate::getImageConfig, query.getImageConfig())
                .eq(UnionPromotionTemplate::getPriority, query.getPriority())
                .eq(UnionPromotionTemplate::getStatus, query.getStatus())
        ;
        Page<UnionPromotionTemplate> page = promotionTemplateMapper.selectPageX(query, queryWrapper);
        List<UnionPromotionTemplateVO> list = converter.convert(page.getRecords(), UnionPromotionTemplateVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
