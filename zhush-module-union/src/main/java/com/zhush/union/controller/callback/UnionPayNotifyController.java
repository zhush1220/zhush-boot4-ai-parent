package com.zhush.union.controller.callback;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.notify.PayNotifyDispatcher;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.satoken.annotation.IgnoreAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道出款结果回调控制器
 *
 * <p>协议无关：原始报文 + 请求头交给 {@code PayNotifyDispatcher} 按渠道解析（强制验签/解密）后，
 * 路由到业务侧 {@code PayNotifyHandler} 收敛业务单状态。
 *
 * <p><b>应答约定</b>：解析/验签/处理任一环节失败必须返回 <b>HTTP 500</b> 触发渠道重试
 * （业务侧已用 CAS + 幂等保护，重复回调安全）；成功返回渠道约定的 ACK 报文。
 *
 * @author zhush
 */
@Slf4j
@RestController
@Tag(name = "支付回调", description = "渠道支付/转账结果回调（免登录，强制验签）")
@RequestMapping("/union/callback")
public class UnionPayNotifyController {

    /** 微信 V3 成功应答报文 */
    private static final String WX_V3_SUCCESS_ACK = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";

    private final PayNotifyDispatcher payNotifyDispatcher;

    public UnionPayNotifyController(PayNotifyDispatcher payNotifyDispatcher) {
        this.payNotifyDispatcher = payNotifyDispatcher;
    }

    /**
     * 微信商家转账（V3 transfer-bills）结果回调
     */
    @PostMapping("/wechat/transfer")
    @IgnoreAuth
    @Operation(summary = "微信商家转账结果回调")
    public ResponseEntity<String> wechatTransfer(@RequestBody String body, HttpServletRequest request) {
        return dispatch(PayChannelType.WECHAT_V3, body, request);
    }

    private ResponseEntity<String> dispatch(PayChannelType channelType, String body, HttpServletRequest request) {
        try {
            PayNotifyMessage message = payNotifyDispatcher.dispatch(channelType, body, collectHeaders(request));
            log.info("[PayNotify] 回调处理完成 channel={}，bizNo={}，state={}",
                    channelType, message.getBizNo(), message.getPayStatus());
            return ResponseEntity.ok(WX_V3_SUCCESS_ACK);
        } catch (Exception e) {
            // 失败返 500 触发渠道重试；业务收敛已幂等，重复通知安全
            log.error("[PayNotify] 回调处理失败 channel={}", channelType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL");
        }
    }

    private Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

}
