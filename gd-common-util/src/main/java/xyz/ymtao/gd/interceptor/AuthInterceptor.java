package xyz.ymtao.gd.interceptor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import redis.clients.jedis.Jedis;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.common.util.HttpclientUtil;
import xyz.ymtao.gd.service.util.RedisUtil;
import xyz.ymtao.gd.web.util.CookieUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    RedisUtil redisUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws IOException, ServletException {

        String userService="http://123.56.175.117:8080";
        //判断被拦截的请求的访问的方法的注解，以判断是否需要被拦截
        HandlerMethod hm=null;
        try{
            hm=(HandlerMethod)handler;
        }catch (ClassCastException e){
            System.out.println("拦截处理器类型转换异常,非拦截目标");
            return true;
        }
        //获取方法上的登录拦截声明注解
        LoginRequired methodAnnotation=hm.getMethodAnnotation(LoginRequired.class);
        //如果没有登录注解，不需要拦截验证token，直接返回true放行
        if(methodAnnotation==null){
            return true;
        }
        //需要拦截访问,验证token，判断是否已登录
        StringBuffer requestURL = request.getRequestURL();
        String token="";
        //获取客户端IP
        String ip=request.getHeader("x-forwarded-for");
        if(StringUtils.isBlank(ip)){
            //如果非nginx转发
            ip=request.getRemoteAddr();
            if(StringUtils.isBlank(ip)){
                ip="127.0.0.1";
            }
        }
        System.out.println("访问拦截，访问IP："+ip+"=====访问url: "+requestURL);
        //获取redis中已有的token
        Jedis jedis=null;
        String oldToken="";
        //获得当前请求方法的LoginReuired注解的loginSuccess参数，已获悉是否必须登录
        boolean loginSuccess=methodAnnotation.loginSuccess();
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
                oldToken=jedis.get("user:"+ip+":token");
            }
        }catch (Exception e){
            System.out.println("redis服务器异常");
            //如果非必须登录，仍可访问
            if(loginSuccess==false){
                return true;
            }
            return  false;
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        if(StringUtils.isNotBlank(oldToken)){
            System.out.println("从redis中获取到token");
            token=oldToken;
        }
        //如果用户重新登录了，获取请求中携带的新token
        String newToken=null;
        newToken=request.getParameter("token");
        if(StringUtils.isNotBlank(newToken)){
            System.out.println("用户重新登录，从请求域中获取到token");
                token=newToken;
        }

        //调用认证中心进行验证token,默认验证失败
        String success="fail";
        Map<String,String> successMap=new HashMap<>();
        //对token进行验证
        if(StringUtils.isNotBlank(token)){
            String successJson= HttpclientUtil.doGet(userService+"/token_verify?token="+token+"&currentIp="+ip);
            successMap = JSON.parseObject(successJson,Map.class);
            success = successMap.get("status");
        }
        //如果必须要登录
        if(loginSuccess){
            System.out.println("need to login!!!");
            //如果token验证失败,重定向到登录页
            if(!success.equals("success")){
                String queryString = request.getQueryString();
                if(queryString!=null){
                    requestURL.append("?"+queryString);
                }
                response.sendRedirect(userService+"/user/login?ReturnUrl="+requestURL);
                return false;
            }
            //已登录，token验证成功，将用户信息写入请求域
            if(request.getAttribute("userId")==null){
                request.setAttribute("userId",successMap.get("userId"));
                request.setAttribute("username",successMap.get("username"));
            }
        }
        //没有登录也可以访问
        else {
            //如果已登录，将token携带的用户信息先放入请求域
            if(success.equals("success")){
                if(request.getAttribute("userId")==null){
                    System.out.println("将用户信息添加到请求域");
                    request.setAttribute("userId",successMap.get("userId"));
                    request.setAttribute("username",successMap.get("username"));
                }
            }
            else{
                System.out.println("非必须登录");
            }
        }
        //如果重新登录产生了新token，覆盖redis中的token
        if (StringUtils.isNotBlank(newToken)){
            System.out.println("覆盖token");
            jedis.setex("user:"+ip+":token",3600,newToken);
        }
        return true;
    }
}
