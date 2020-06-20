package xyz.ymtao.gd.order_pay.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import xyz.ymtao.gd.entity.Orders;
import xyz.ymtao.gd.order_pay.service.OrderServiceImpl;
import xyz.ymtao.gd.service.OrderService;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYHMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no=mapMessage.getString("out_trade_no");
        //更新订单业务状态
        System.out.println("更新订单业务状态,外部订单号："+out_trade_no);
        Orders order=new Orders();
        order.setOrderSn(out_trade_no);
        order.setStatus(1);
        orderService.updateOrder(order);
    }
}
