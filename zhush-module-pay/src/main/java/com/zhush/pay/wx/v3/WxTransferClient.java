package com.zhush.pay.wx.v3;

import com.github.binarywang.wxpay.bean.transfer.TransferBillsCancelResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsGetResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsRequest;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.wx.WxPayProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信 V3 商家转账客户端（transfer-bills）
 *
 * <p>封装 SDK {@code WxPayService#getTransferService()} 的三个能力：
 * 转账 / 查单 / 撤单。仅做协议调用与异常透出，不含业务状态机。
 *
 * <p>接口文档：{@code /v3/fund-app/mch-transfer/transfer-bills}
 *
 * @author zhush
 */
@Slf4j
public class WxTransferClient {

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxTransferClient(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    /**
     * 发起转账
     */
    public TransferBillsResult transfer(TransferBillsRequest request) throws WxPayException {
        return wxPayService.getTransferService().transferBills(request);
    }

    /**
     * 按商户单号查单
     */
    public TransferBillsGetResult queryByOutBillNo(String outBillNo) throws WxPayException {
        return wxPayService.getTransferService().getBillsByOutBillNo(outBillNo);
    }

    /**
     * 按微信转账单号查单
     */
    public TransferBillsGetResult queryByTransferBillNo(String transferBillNo) throws WxPayException {
        return wxPayService.getTransferService().getBillsByTransferBillNo(transferBillNo);
    }

    /**
     * 撤销转账
     */
    public TransferBillsCancelResult cancel(String outBillNo) throws WxPayException {
        return wxPayService.getTransferService().transformBillsCancel(outBillNo);
    }

    /**
     * 默认商户号（多商户路由预留）
     */
    public String defaultMchId() {
        return properties.getV3().getMchId();
    }

    /**
     * 默认 AppID
     */
    public String defaultAppId() {
        return properties.getV3().getAppId();
    }

}
