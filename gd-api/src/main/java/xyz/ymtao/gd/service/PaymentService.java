package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.PaymentInfo;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    Map<String, Object> checkAlipayment(String out_trade_no);

    void sendDelayPaymentResultCheckQueue(String out_trade_no, Integer count);

    String getAlipayForm(String outTradeNo, BigDecimal totalAmount);
}
