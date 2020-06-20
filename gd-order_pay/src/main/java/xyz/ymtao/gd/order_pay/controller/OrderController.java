package xyz.ymtao.gd.order_pay.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.entity.Cart;
import xyz.ymtao.gd.entity.OrderCommodityInfo;
import xyz.ymtao.gd.entity.Orders;
import xyz.ymtao.gd.entity.UserReceiveAddress;
import xyz.ymtao.gd.service.CartService;
import xyz.ymtao.gd.service.OrderService;
import xyz.ymtao.gd.service.SkuService;
import xyz.ymtao.gd.service.UserService;
import xyz.ymtao.gd.web.util.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@CrossOrigin
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Autowired
    OrderService orderService;

    @Reference
    SkuService skuService;


    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(Orders order, String tradeCode, HttpServletRequest request, HttpServletResponse response) {

        String memberId = (String) request.getAttribute("userId");
        String username=(String)request.getAttribute("username");

        // 检查交易码
        String success = orderService.checkTradeCode(memberId, tradeCode);

        if (success.equals("success")) {
            List<OrderCommodityInfo> omsOrderItems = new ArrayList<>();
            // 创建订单对象
            Orders omsOrder = new Orders();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时
            omsOrder.setUserId(memberId);
            omsOrder.setStatus(0);
            omsOrder.setNote(order.getNote());
            String outTradeNo = "yujuge";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号

            omsOrder.setOrderSn(outTradeNo);//外部订单号
            omsOrder.setPayAmount(order.getTotalAmount());
            omsOrder.setReceiveAddressId(order.getReceiveAddressId());
            // 当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setStatus(0);
            omsOrder.setTotalAmount(order.getTotalAmount());

            // 根据用户id从缓存获得要购买的商品列表(购物车)，和总价格
            List<Cart> omsCartItems = cartService.getCartListFromCache(memberId);
            //如果从缓存中获取失败,则从数据库获取
            if(omsCartItems==null){
                omsCartItems=cartService.getCartFromDb(memberId);
            }

            for (Cart omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")) {
                    System.out.println("选中该商品结算=============="+omsCartItem.getCommoditySkuId());
                    // 获得订单详情列表
                    OrderCommodityInfo omsOrderItem = new OrderCommodityInfo();
                    // 检价
                    boolean b = skuService.checkPrice(omsCartItem.getCommoditySkuId(),omsCartItem.getPrice());
                    if (b == false) {
                        System.out.println("价格错误");
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }
                    // 验库存,远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getCommodityPicture());
                    omsOrderItem.setProductName(omsCartItem.getCommodityName());

                    omsOrderItem.setOrderSn(outTradeNo);// 外部订单号，用来和其他系统进行交互，防止重复
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("111111111111");
                    omsOrderItem.setProductSkuId(omsCartItem.getCommoditySkuId());
                    System.out.println("order=============="+omsCartItem.getCommoditySkuId());
                    omsOrderItem.setProductId(omsCartItem.getCommodityId());
                    omsOrderItem.setProductSn("仓库对应的商品编号");// 在仓库中的skuId

                    omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);

            // 将订单和订单详情写入数据库
            // 删除购物车的对应商品
            orderService.saveOrder(omsOrder);

            //更新购物车cookie
            List<Cart> cartList=cartService.getCartFromDb(memberId);
            String cartStr= JSON.toJSONString(cartList);
            CookieUtil.setCookie(request,response,"cartListCookie", cartStr,60*60*72,true);

            //更新购物车缓存
            cartService.flushCartCache(memberId);

            // 重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:/payment/index");
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",order.getTotalAmount());
            mv.addObject("username",username);
            mv.addObject("orderId",omsOrder.getId());
            return mv;
        } else {
            System.out.println("交易码错误");
            ModelAndView mv = new ModelAndView("tradeFail");
            mv.addObject("errMsg","交易码错误");
            return mv;
        }

    }


    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("userId");
        String nickName=(String)request.getAttribute("username");
        // 收件人地址列表
        List<UserReceiveAddress> umsMemberReceiveAddresses = userService.getReceiveAddressByUserId(memberId);

        // 将购物车集合转化为页面计算清单集合
        List<Cart> omsCartItems = cartService.getCartListFromCache(memberId);
        if(omsCartItems==null || omsCartItems.size()==0){
            System.out.println("缓存中暂无购物车数据");
            omsCartItems=cartService.getCartFromDb(memberId);
        }

        List<OrderCommodityInfo> omsOrderItems = new ArrayList<>();

        for (Cart omsCartItem : omsCartItems) {
            //从所有购物车数据中选出选中结算的商品
            // 每循环一个购物车对象，就封装一个商品的详情到OmsOrderItem
            if (omsCartItem.getIsChecked().equals("1")) {
                OrderCommodityInfo omsOrderItem = new OrderCommodityInfo();
                omsOrderItem.setProductName(omsCartItem.getCommodityName());
                omsOrderItem.setProductPic(omsCartItem.getCommodityPicture());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItems.add(omsOrderItem);
            }
        }

        modelMap.put("omsOrderItems", omsOrderItems);
        modelMap.put("userAddressList", umsMemberReceiveAddresses);
        modelMap.put("totalAmount", getTotalAmount(omsCartItems));
        modelMap.put("nickName",nickName);
        // 生成交易码，为了在提交订单时做交易码的校验
        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }

    @RequestMapping("/order/test/{outTradeNo}")
    @ResponseBody
    public String ordertest(@PathVariable("outTradeNo") String outTradeNo){
        Orders orders=new Orders();
        orders.setOrderSn(outTradeNo);
        orderService.updateOrder(orders);
        return "修改订单测试";
    }

    private BigDecimal getTotalAmount(List<Cart> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (Cart omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getPrice().multiply(omsCartItem.getQuantity());

            if (omsCartItem.getIsChecked().equals("1")) {
                totalAmount = totalAmount.add(totalPrice);
            }
        }

        return totalAmount;
    }


}
