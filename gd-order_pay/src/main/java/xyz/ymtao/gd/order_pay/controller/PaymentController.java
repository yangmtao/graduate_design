package xyz.ymtao.gd.order_pay.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.entity.Orders;
import xyz.ymtao.gd.entity.PaymentInfo;
import xyz.ymtao.gd.service.OrderService;
import xyz.ymtao.gd.service.PaymentService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @Autowired
    OrderService orderService;

    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String aliPayCallBackReturn(HttpServletRequest request, ModelMap modelMap){

        String userName=(String)request.getAttribute("username");
        // 回调请求中获取支付宝参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();
        System.out.println("alipay/callback,sign: "+sign);
        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if(StringUtils.isNotBlank(sign)){
            // 验签成功
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            // 更新用户的支付状态,同时通知订单服务修改订单状态
            paymentService.updatePayment(paymentInfo);
        }
        modelMap.put("totalAmount",total_amount);
        modelMap.put("nickName",userName);
        modelMap.put("tradeNo",trade_no);
        return "finish";
    }



    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount){
        System.out.println("alipay/submit:outTradeNo:"+outTradeNo+"totalAmount:"+totalAmount);
        String form=paymentService.getAlipayForm(outTradeNo,totalAmount);
        // 提交请求到支付宝
        if(form!=null){
            //向消息中间件发送一个检查支付状态的延迟队列消息
            paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);
            return form;
        }
        return "服务器出了点意外，请稍后再试";
    }

    @RequestMapping("/payment/index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){
        String memberId = (String)request.getAttribute("userId");
        String nickname = (String)request.getAttribute("username");
        Orders orders=orderService.getOrderByOutTradeNo(outTradeNo);
        System.out.println("总价格："+totalAmount);
        modelMap.put("username",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("orderId",orders.getId());

        return "payment_index";
    }


    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        return null;
    }

}
