package com.zhush.pay.core.notify;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 统一回调消息（协议无关）
 *
 * <p>由各协议的 {@link PayNotifyParser} 验签/解密后产出，业务侧 {@link PayNotifyHandler}
 * 只消费本模型，不感知 XML/JSON、签名算法等差异。
 *
 * @author zhush
 */
@Data
@Builder
public class PayNotifyMessage {

    /** 渠道类型（协议） */
    private PayChannelType channelType;

    /** 通知类型（转账/支付/退款） */
    private PayNotifyType notifyType;

    /** 业务单号（渠道幂等键，与请求时一致） */
    private String bizNo;

    /** 渠道订单号 */
    private String channelOrderNo;

    /** 统一状态 */
    private PayStatus payStatus;

    /** 渠道原始状态字符串（脱敏） */
    private String channelState;

    /** 实际金额（单位：分；可能为空） */
    private Integer amount;

    /** 商户号（用于校验报文归属，防止串商户伪造） */
    private String mchId;

    /** 渠道完成时间 */
    private LocalDateTime successTime;

    /** 失败原因 */
    private String failReason;

}
