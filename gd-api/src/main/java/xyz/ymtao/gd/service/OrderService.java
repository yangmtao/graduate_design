package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.OrderCommodityInfo;
import xyz.ymtao.gd.entity.Orders;

import java.util.List;

public interface OrderService {
    String checkTradeCode(String memberId, String tradeCode);

    void saveOrder(Orders omsOrder);

    String genTradeCode(String memberId);

    Orders getOrderByOutTradeNo(String outTradeNo);

    List<OrderCommodityInfo> getOrderSkuByOrderSn(String orderSn);

    void updateOrder(Orders omsOrder);

    List<Orders> getOrderByUserId(String userId);

    void deleteOrder(Orders order);
}
