package xyz.ymtao.gd.order_pay.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import xyz.ymtao.gd.entity.PaymentInfo;
import xyz.ymtao.gd.service.PaymentService;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Map;

@Component
public class PaymentServiceMqListener {
    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no=mapMessage.getString("out_trade_no");
        Integer count=0;
        if(mapMessage.getString("count")!=null){
            count=Integer.parseInt(mapMessage.getString("count"));
        }
        //调用paymentService的支付宝检查接口
        System.out.println("paymentServiceListener:进行延迟检查:"+out_trade_no+"，调用支付宝的接口服务");
        Map<String,Object> resultMap=paymentService.checkAlipayment(out_trade_no);
        if(resultMap!=null && !resultMap.isEmpty()){
            String trade_status=(String)resultMap.get("trade_status");
            //根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功跟新数据和后续任务
            if(StringUtils.isNotBlank(trade_status)&&trade_status.equals("TRADE_SUCCESS")){
                //支付成功，更新支付发送到支付队列
                PaymentInfo paymentInfo=new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                //支付宝交易凭证号
                paymentInfo.setAlipayTradeNo((String)resultMap.get("trade_no"));
                paymentInfo.setCallbackContent((String)resultMap.get("call_back_content"));
                System.out.println("支付已成功，调用支付服务，修改支付消息和发送支付成功的队列");
                paymentService.updatePayment(paymentInfo);
                return;
            }
        }

        if(count>0){
            //继续执行延迟队列检查任务，计算延迟时间等
            System.out.println("暂未完成支付，剩余检查次数为"+count);
            count--;
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no,count);
        }else{
            System.out.println("检查次数用尽，默认支付失败");

        }
    }
}
