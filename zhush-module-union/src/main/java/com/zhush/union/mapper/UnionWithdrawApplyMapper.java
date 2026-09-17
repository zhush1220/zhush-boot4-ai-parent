package com.zhush.union.mapper;


import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionWithdrawApply;

import java.util.List;

/**
 * @ClassName UnionWithdrawApplyMapper
 * @Description 提现申请表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionWithdrawApplyMapper extends BaseMapperX<UnionWithdrawApply> {

    /**
     * 按申请单号反查（单号业务唯一，作渠道幂等键；回调/查单据此定位单据）
     *
     * @param applyNo 申请单号
     * @return 提现申请，不存在返回 {@code null}
     */
    default UnionWithdrawApply selectByApplyNo(String applyNo) {
        List<UnionWithdrawApply> list = selectListByQuery(
                QueryWrapper.create().where(UnionWithdrawApply::getApplyNo).eq(applyNo));
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }

    /**
     * 按推广者 + 创建时间区间取提现单（用于单日累计限额汇总）
     *
     * @param memberId 推广者ID
     * @param start    区间起（含）
     * @param end      区间止（不含）
     * @return 提现单列表
     */
    default List<UnionWithdrawApply> selectListByMemberAndCreateTime(Long memberId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end) {
        return selectListByQuery(QueryWrapper.create()
                .where(UnionWithdrawApply::getMemberId).eq(memberId)
                .and(UnionWithdrawApply::getCreateTime).ge(start)
                .and(UnionWithdrawApply::getCreateTime).lt(end));
    }

}
