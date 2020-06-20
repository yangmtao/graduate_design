package xyz.ymtao.gd.order_pay.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;
import xyz.ymtao.gd.entity.OrderCommodityInfo;
import xyz.ymtao.gd.entity.Orders;
import xyz.ymtao.gd.order_pay.mapper.OrderCommodityInfoMapper;
import xyz.ymtao.gd.order_pay.mapper.OrderMapper;
import xyz.ymtao.gd.service.CartService;
import xyz.ymtao.gd.service.OrderService;
import xyz.ymtao.gd.service.util.ActiveMQUtil;
import xyz.ymtao.gd.service.util.RedisUtil;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderMapper omsOrderMapper;

    @Autowired
    OrderCommodityInfoMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null ;

        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";


            //String tradeCodeFromCache = jedis.get(tradeKey);// 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
            //对比防重删令牌
           String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
           Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval!=null&&eval!=0) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }
        }finally {
            jedis.close();
        }

    }

    @Override
    public String genTradeCode(String memberId) {

        Jedis jedis = redisUtil.getJedis();

        String tradeKey = "user:"+memberId+":tradeCode";

        String tradeCode = UUID.randomUUID().toString();

        jedis.setex(tradeKey,60*15,tradeCode);

        jedis.close();

        return tradeCode;
    }

    //根据外部订单号获取订单
    @Override
    public Orders getOrderByOutTradeNo(String outTradeNo) {
       // Example example=new Example(Orders.class);
        //example.createCriteria().andEqualTo("order_sn",outTradeNo);
        System.out.println("outTradeNo"+outTradeNo);
        Orders order=new Orders();
        order.setOrderSn(outTradeNo);
        return omsOrderMapper.selectOne(order);
    }

    //更新订单状态，同时通知库存服务
    @Override
    public void updateOrder(Orders omsOrder) {
        Orders order=new Orders();
        order.setStatus(omsOrder.getStatus());
        Example example=new Example(Orders.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());
        //如果当前需更新得订单得订单状态未0
        if(omsOrder!=null&&omsOrder.getStatus()==0){
            //发送一个订单已支付的消息，提供给库存
            Connection connection=null;
            Session session=null;
            try{
                connection=  activeMQUtil.getConnectionFactory().createConnection();
                session=connection.createSession(true,Session.SESSION_TRANSACTED);
                Queue payment_success_queue=session.createQueue("ORDER_PAY_QUEUE");
                MessageProducer messageProducer=session.createProducer(payment_success_queue);
                TextMessage textMessage=new ActiveMQTextMessage();
                //查询该订单对象详细信息，转化为JSON字符串，存入ORDER_PAY_QUEUE消息队列
                Orders orderParam=new Orders();
                orderParam.setOrderSn(omsOrder.getOrderSn());
                Orders orderResponse=omsOrderMapper.selectOne(orderParam);

                OrderCommodityInfo orderItemParam=new OrderCommodityInfo();
                orderItemParam.setOrderSn(omsOrder.getOrderSn());
                List<OrderCommodityInfo> orderCommodityInfos=omsOrderItemMapper.select(orderItemParam);
                orderResponse.setOmsOrderItems(orderCommodityInfos);
                textMessage.setText(JSON.toJSONString(orderResponse));
                //发送消息
                messageProducer.send(textMessage);
                session.commit();
            } catch (JMSException e) {
                e.printStackTrace();
                try{
                    session.rollback();
                }catch (JMSException ekms){
                    ekms.printStackTrace();
                }
            }finally {
                try{
                    connection.close();
                }catch (JMSException jmse){
                    jmse.printStackTrace();
                }
            }
        }
        //更新订单
        omsOrderMapper.updateByExampleSelective(order,example);
        System.out.println("订单已更新");

    }

    //根据用户Id获取订单详情
    @Override
    public List<Orders> getOrderByUserId(String userId) {
        List<Orders> orderList=null;
        Example example=new Example(Orders.class);
        example.createCriteria().andEqualTo("userId",userId);
        orderList=omsOrderMapper.selectByExample(example);
        if(orderList!=null){

            for (Orders order:orderList) {
                Example itemExample=new Example(OrderCommodityInfo.class);
                itemExample.createCriteria().andEqualTo("orderId",order.getId());
                List<OrderCommodityInfo> commodityInfoList=omsOrderItemMapper.selectByExample(itemExample);
                order.setOrderName(commodityInfoList.get(0).getProductName()+"...等"+commodityInfoList.size()+"件商品");
                System.out.println("orderName:"+order.getOrderName());
                order.setOmsOrderItems(commodityInfoList);
            }
        }

        return orderList;
    }

    //根据orderSn获取订单商品信息
    @Override
    public List<OrderCommodityInfo> getOrderSkuByOrderSn(String orderSn){
        OrderCommodityInfo orderCommodityInfo=new OrderCommodityInfo();
        orderCommodityInfo.setOrderSn(orderSn);
        List<OrderCommodityInfo> orderCommodityInfoList=omsOrderItemMapper.select(orderCommodityInfo);
        return orderCommodityInfoList;
    }

    //删除订单
    @Override
    public void deleteOrder(Orders order) {
        omsOrderMapper.delete(order);
        OrderCommodityInfo commodityInfo=new OrderCommodityInfo();
        commodityInfo.setOrderSn(order.getOrderSn());
        omsOrderItemMapper.delete(commodityInfo);
    }

    //保存订单详情，同时删除对应购物车
    @Override
    public void saveOrder(Orders omsOrder) {

        // 保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        // 保存订单商品信息
        List<OrderCommodityInfo> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OrderCommodityInfo omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            // 删除购物车数据
            System.out.println("productskuid : "+omsOrderItem.getProductSkuId());
             cartService.delCart(omsOrderItem.getProductSkuId());
        }
    }


}
