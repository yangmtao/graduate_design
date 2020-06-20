package xyz.ymtao.gd.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.common.util.MD5Utils;
import xyz.ymtao.gd.entity.*;
import xyz.ymtao.gd.service.CartService;
import xyz.ymtao.gd.service.OrderService;
import xyz.ymtao.gd.service.UserService;
import xyz.ymtao.gd.user.util.JwtUtil;
import xyz.ymtao.gd.web.util.CookieUtil;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Controller
@CrossOrigin
public class UserController {
    @Autowired
    UserService userService;
    @Reference
    CartService cartService;
    @Reference
    OrderService orderService;

    @RequestMapping("/login")
    @ResponseBody
    public String login(User user, HttpServletRequest request,HttpServletResponse response){
        String token="";
        String md5Password= MD5Utils.md5(user.getPassword());
        System.out.println("md5Password is: "+md5Password);
        user.setPassword(md5Password);
        //调用用户服务验证手机号和密码
        User userLogined=userService.login(user);
        if(userLogined!=null){
            //登录成功
            String userId=userLogined.getId();
            System.out.println("userId is:"+userId);
            String username=userLogined.getUsername();
            Map<String,Object> userMap=new HashMap<>();
            userMap.put("userId",userId);
            userMap.put("username",username);
            //获取通过nginx转发的客户端ip
            String ip=request.getHeader("x-forward-for");
            if(StringUtils.isBlank(ip)){
                //如果非nginx转发的请求,从request中获取IP
                ip=request.getRemoteAddr();
                //如果非外地客户端访问
                if(StringUtils.isBlank(ip)){
                    ip="127.0.0.1";
                }
            }
            System.out.println("登录客户端ip:"+ip);
            //按照设计的算法对参数进行加密生成token,并关联用户信息
            token= JwtUtil.encode("gd",userMap,ip);
            //将token存入缓存
            userService.addUserToken(token,ip);
            //获取cookie里原有的购物车数据
            /*String cartListCookie= CookieUtil.getCookieValue(request,"cartListCookie",true);
            if(StringUtils.isNotBlank(cartListCookie)){
                //如果cookie中的购物车信息不为空，合并购物车
                List<Cart> carts= cartService.mergeCart(userId,JSON.parseArray(cartListCookie, Cart.class));
                if(carts!=null&&carts.size()!=0){
                    //更新cookie
                    CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(carts),60*60*72,true);
                }
            }*/
        }else{
            //登录失败
            token="fail";
        }
        return token;
    }

    //用户注册
    @RequestMapping(value="/user/register",method= RequestMethod.POST)
    @ResponseBody
    public Map<String, String> userRegister(User user, String sms)throws ServletException,IOException{
        Map<String,String> returnMap=userService.register(user,sms);
        //返回注册请求结果
        return  returnMap;
    }

    //验证图形验证码并获取短信验证码
    @RequestMapping(value="/getSms",method= RequestMethod.GET)
    @ResponseBody
    public Map<String, String> sendSms(String telNumber, String checkCode, HttpServletRequest request) throws IOException, ServletException {
        Map<String,String> returnMap=new HashMap<>();
        //获取通过nginx转发的客户端ip
        String ip=request.getHeader("x-forward-for");
        if(StringUtils.isBlank(ip)){
            //如果非nginx转发的请求,从request中获取IP
            ip=request.getRemoteAddr();
            //如果非外地客户端访问
            if(StringUtils.isBlank(ip)){
                ip="127.0.0.1";
            }
        }
        User user=userService.getUserByPhone(telNumber);
        if(user!=null){
            returnMap.put("details","该手机号已注册！");
            return returnMap;
        }
        //验证图形验证码
       String imageCheck= userService.checkImageCode(checkCode,ip);
        //图形验证码验证成功，再发短信验证码
        if(imageCheck.equals("success")){
            String sms=userService.getMessageCode(telNumber);
            System.out.println("sms is: "+sms);
            if(sms!=null){
                returnMap.put("details","短信验证码已发送！");
            }
        }
        else{
            returnMap.put("details","短信验证码发送失败，请检查手机号或图形验证码是否输入正确");
        }
       return returnMap;
    }

    @RequestMapping("/token_verify")
    @ResponseBody
    public String tokenVerify(String token,String currentIp,HttpServletRequest request){
        //通过jwt校验token真假
        Map<String,String> map=new HashMap<>();
        Map<String,Object> decode= JwtUtil.decode(token,"gd",currentIp);
        //token验证成功
        if(decode!=null){
            map.put("status","success");
            map.put("userId",(String)decode.get("userId"));
            map.put("username",(String)decode.get("username"));
        }
        //token验证失败
        else{
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }

    @RequestMapping("/user/login")
    @LoginRequired(loginSuccess = false)
    public String index(HttpServletRequest request, ModelMap modelMap){
        String queryStr=request.getQueryString();
        String ReturnUrl=null;
        if(queryStr!=null){
            ReturnUrl=queryStr.substring(10);
        }
        modelMap.put("ReturnUrl",ReturnUrl);
        return "login";
    }

    //获取图形验证码
    @RequestMapping(value="/getImageCode",method= RequestMethod.GET)
    public void getCheckCode(HttpServletRequest request,HttpServletResponse response) throws IOException {
        //获取请求客户端IP
        String ip=request.getHeader("x-forwarded-for");
        if(StringUtils.isBlank(ip)){
            //如果非nginx转发
            ip=request.getRemoteAddr();
            if(StringUtils.isBlank(ip)){
                ip="127.0.0.1";
            }
        }
        //获取图形验证码
        ImageCodeInfo imageCodeInfo=userService.getImageCode(ip);
        if (imageCodeInfo!=null){
            ByteArrayInputStream in=new ByteArrayInputStream(imageCodeInfo.getImageBytes());
            BufferedImage bufferedImage=ImageIO.read(in);
            ImageIO.write(bufferedImage, "JPEG", response.getOutputStream());
        }
    }
    @RequestMapping("/user/center")
    @LoginRequired(loginSuccess = true)
    public String userCenter(HttpServletRequest request,ModelMap modelMap){
        Long userId=Long.valueOf((String)request.getAttribute("userId"));
        User user=userService.getUserById(userId);
        modelMap.put("user",user);
        modelMap.put("navName","基本信息");
        modelMap.put("case","user_info");
        return "user";
    }

    @RequestMapping("/user/modify")
    public String userModify(@RequestParam("userId") Long userId, ModelMap modelMap){
        User user=userService.getUserById(userId);
        modelMap.put("user",user);
        modelMap.put("navName","基本信息 >>> 修改");
        modelMap.put("case","user_modify");
        return "user";
    }

    @RequestMapping("/user/receiveAddress")
    @LoginRequired(loginSuccess = true)
    public String receiveAddress(HttpServletRequest request, ModelMap modelMap){
        Long userId=Long.valueOf((String)request.getAttribute("userId"));
        List<UserReceiveAddress> addressList=userService.getReceiveAddressByUserId(Long.toString(userId));
        modelMap.put("navName","收货地址");
        modelMap.put("case","receive_address");
        modelMap.put("addressList",addressList);
        modelMap.put("userId",userId);
        return "user";
    }

    @RequestMapping("/address/add.html")
    public String addressAdd(@RequestParam("userId") Long userId, ModelMap modelMap){
         modelMap.put("case","address_add_page");
         modelMap.put("navName","添加收货地址");
         modelMap.put("userId",userId);
         return "user";
    }

    @RequestMapping("/address/add")
    public String addAddress(UserReceiveAddress receiveAddress, ModelMap modelMap){
        int i=userService.addReceiveAddress(receiveAddress);
        List<UserReceiveAddress> addressList=userService.getReceiveAddressByUserId(receiveAddress.getUserId());
        modelMap.put("navName","收货地址");
        modelMap.put("case","receive_address");
        modelMap.put("addressList",addressList);
        return "user";
    }

    @RequestMapping("/user/update")
    public String userUpdate(User user, ModelMap modelMap){
        int i=userService.updateUserByUserId(user);
        User newUser=userService.getUserById(Long.valueOf(user.getId()));
        modelMap.put("user",user);
        modelMap.put("navName","基本信息");
        modelMap.put("case","user_info");
        return "user";
    }

    @RequestMapping("/user/password")
    public String userPassword(ModelMap modelMap){
        modelMap.put("case","password");
        return "user";
    }

    @RequestMapping("/user/order/{type}")
    @LoginRequired(loginSuccess = true)
    public String userOrder(@PathVariable("type") String type,HttpServletRequest request,ModelMap modelMap){
        String userId=(String)request.getAttribute("userId");
        List<Orders> orderList=new ArrayList<>();

        if(type!=null){
            List<Orders>  orderListCopy =orderService.getOrderByUserId(userId);
            switch (type){
                case "all":
                    orderList=orderListCopy;
                    modelMap.put("orderCase","all");
                    break;

                case "unPay":
                    for (Orders order:orderListCopy) {
                        if(order.getStatus()==0){
                            orderList.add(order);
                        }
                    }
                    modelMap.put("orderCase","unPay");
                    break;
                case "unPost":
                    for (Orders order:orderListCopy) {
                        if(order.getStatus()==1){
                            orderList.add(order);
                        }
                    }
                    modelMap.put("orderCase","unPost");
                    break;
                case "unConfirm":
                    for (Orders order:orderListCopy) {
                        if(order.getStatus()==2){
                            orderList.add(order);
                        }
                    }
                    modelMap.put("orderCase","unConfirm");
                    break;

                case "complete":
                    for (Orders order:orderListCopy) {
                        if(order.getStatus()==3){
                            orderList.add(order);
                        }
                    }
                    modelMap.put("orderCase","complete");
                    break;

                case "cancled":
                    for (Orders order:orderListCopy) {
                        if(order.getStatus()==4){
                            orderList.add(order);
                        }
                    }
                    modelMap.put("orderCase","cancled");
                    break;

            }
        }
        if(orderList.size()==0){
            modelMap.put("orderList",null);
        }
        else{
            modelMap.put("orderList",orderList);
        }
        modelMap.put("case","order");
        return "user";
    }

    @RequestMapping("/user/order/operate/{type}/{orderSn}")
    @LoginRequired(loginSuccess = true)
    public String userOrderOperate(@PathVariable("type") String type,@PathVariable("orderSn") String orderSn,HttpServletRequest request,ModelMap modelMap){
        String userId=(String)request.getAttribute("userId");
        Orders order=new Orders();
        order.setOrderSn(orderSn);
        System.out.println("userOrderOperate:"+type+orderSn);
        if(type!=null){
            switch (type){
                case "confirm":
                    order.setStatus(3);
                    orderService.updateOrder(order);
                    modelMap.put("case","order");
                    return "redirect:/user/order/complete";

                case "cancle":
                    order.setStatus(4);
                    orderService.updateOrder(order);
                    modelMap.put("case","order");
                    return "redirect:/user/order/cancled";

                case "delete":
                    orderService.deleteOrder(order);
                    modelMap.put("case","order");
                    return "redirect:/user/order/all";

                case "addComment":
                    //查询订单商品信息
                    List<OrderCommodityInfo> orderSkuList=orderService.getOrderSkuByOrderSn(orderSn);
                    modelMap.put("orderSkuList",orderSkuList);
                    modelMap.put("case","order_add_comment");
            }
        }
        return "user";
    }

    @RequestMapping("/user/comment/{type}")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public ModelAndView userComment(@PathVariable("type") String type, UserComment userComment, HttpServletRequest request){
        String userId=(String)request.getAttribute("userId");
        String username=(String)request.getAttribute("username");
        ModelAndView mv=new ModelAndView();
        if(type.equals("add")){
          userComment.setCreateTime(new Date());
          userComment.setUserId(userId);
          userComment.setUserName(username);
          userComment.setStatus(0);
          userComment.setReadCount(1);
          userService.addComment(userComment);
        }else if(type.equals("delete")){
            userService.deleteComment(userComment);
        }
        List<UserComment> userComments=userService.getUserCommentByUserId(userId);
        mv.setViewName("user");
        mv.addObject("case","comment");
        mv.addObject("userCommentList",userComments);
        return mv;
    }

}
