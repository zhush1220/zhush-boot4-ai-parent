package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionMemberAccountQuery
 * @Description 推广者账户表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广者账户表分页查询请求")
public class UnionMemberAccountQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "乐观锁版本号")
    private Integer version;

}
