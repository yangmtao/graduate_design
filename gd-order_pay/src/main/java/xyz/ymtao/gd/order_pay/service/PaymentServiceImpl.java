package xyz.ymtao.gd.order_pay.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;
import xyz.ymtao.gd.order_pay.config.AlipayConfig;
import xyz.ymtao.gd.entity.Orders;
import xyz.ymtao.gd.entity.PaymentInfo;
import xyz.ymtao.gd.order_pay.mapper.PaymentInfoMapper;
import xyz.ymtao.gd.service.OrderService;
import xyz.ymtao.gd.service.PaymentService;
import xyz.ymtao.gd.service.util.ActiveMQUtil;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    OrderService orderService;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }


    //更新支付信息
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        String orderSn = paymentInfo.getOrderSn();
        System.out.println("updatePayment,orderSn:"+orderSn);
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn",orderSn);

        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        try{
            paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
            // 支付成功后，引起的系统服务-》订单服务的更新-》库存服务-》物流服务
            // 调用mq发送支付成功的消息
            Queue payhment_success_queue = session.createQueue("PAYHMENT_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);

            //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本

            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());

            producer.send(mapMessage);

            session.commit();
        }catch (Exception ex){
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }



    }

    //检查支付信息
    @Override
    public Map<String, Object> checkAlipayment(String out_trade_no) {
        Map<String,Object> resultMAp=new HashMap<>();
        AlipayTradeQueryRequest request=new AlipayTradeQueryRequest();
        Map<String,Object> requestMap=new HashMap<>();
        requestMap.put("out_trade_no",out_trade_no);
        String requestStr=JSON.toJSONString(requestMap);
        System.out.println("alipay check requestStr:"+requestStr);
        request.setBizContent(requestStr);
        AlipayTradeQueryResponse response=null;
        try{
            response=alipayClient.execute(request);
        }catch (AlipayApiException e){
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("交易已创建，调用成功");
            resultMAp.put("out_trade_no",response.getOutTradeNo());
            resultMAp.put("trade_no",response.getTradeNo());
            resultMAp.put("trade_status",response.getTradeStatus());
            resultMAp.put("call_back_content",response.getMsg());
        }else{
            System.out.println("交易未创建，交易失败");
        }
        return resultMAp;
    }

    //发送延迟队列支付信息检查
    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, Integer count) {
        Connection connection=null;
        Session session=null;
        try{
            connection=activeMQUtil.getConnectionFactory().createConnection();
            session=connection.createSession(true,Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try{
            Queue payhment_success_queue=session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer=session.createProducer(payhment_success_queue);
            MapMessage mapMessage=new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setString("count", String.valueOf(count));
            //为消息加入延迟时间,延迟一个小时
            Long time=1000*60*60L;
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,time);
            producer.send(mapMessage);
            session.commit();
        } catch (JMSException e) {
            //消息回滚
            try{
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }

    //获取支付宝支付表单
    @Override
    public String getAlipayForm(String outTradeNo, BigDecimal totalAmount) {
        // 获得一个支付宝请求的客户端(它并不是一个链接，而是一个封装好的http的表单请求)
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",totalAmount);
        map.put("subject","渔具阁商品");

        String param = JSON.toJSONString(map);

        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 生成并且保存用户的支付信息
        Orders omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("谷粒商城商品一件");
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfo.setAlipayTradeNo("00001");
        savePaymentInfo(paymentInfo);

        return form;
    }
}
