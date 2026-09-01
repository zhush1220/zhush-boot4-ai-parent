package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告查看记录分页查询请求（按公告维度查询，见 controller 路径参数 noticeId）
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "公告查看记录分页查询请求")
public class NoticeRecordQuery extends BaseQuery {

    @Schema(description = "查看人姓名（模糊匹配）")
    private String userName;
}
