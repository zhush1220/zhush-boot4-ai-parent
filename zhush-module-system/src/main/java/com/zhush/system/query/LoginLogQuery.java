package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 登录日志分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "登录日志分页查询请求")
public class LoginLogQuery extends BaseQuery {

    @Schema(description = "登录账号（模糊匹配）")
    private String username;

    @Schema(description = "登录IP（精确匹配）")
    private String loginIp;

    @Schema(description = "是否成功：true-仅成功 false-仅失败，不传查全部")
    private Boolean success;

    @Schema(description = "登录时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeBegin;

    @Schema(description = "登录时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;

}
