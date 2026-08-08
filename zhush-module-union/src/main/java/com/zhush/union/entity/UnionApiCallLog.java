package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionApiCallLog
 * @Description 平台API调用日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_api_call_log")
public class UnionApiCallLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -2082368673027667265L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 平台
     */
    private Integer platform;

    /**
     * API名称（如：taobao.tbk.item.get）
     */
    private String apiName;

    /**
     * 请求参数（脱敏）
     */
    private String requestParams;

    /**
     * 响应码
     */
    private String responseCode;

    /**
     * 响应消息
     */
    private String responseMsg;

    /**
     * 是否成功
     */
    private Boolean isSuccess;

    /**
     * 耗时（毫秒）
     */
    private Integer duration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
