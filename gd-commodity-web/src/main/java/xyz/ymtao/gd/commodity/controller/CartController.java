package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.comparator.CartComparator;
import xyz.ymtao.gd.entity.Cart;
import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.entity.CommoditySkuSaleAttributeValue;
import xyz.ymtao.gd.service.CartService;
import xyz.ymtao.gd.service.SkuService;
import xyz.ymtao.gd.web.util.CookieUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@CrossOrigin
public class CartController {
    
    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;


    @RequestMapping("/checkCart")
    @LoginRequired(loginSuccess = true)
    public String checkCart(String isChecked, String skuId, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        String memberId=(String)request.getAttribute("userId");
        List<Cart> omsCartItems=null;
        if(StringUtils.isNotBlank(memberId)){
            //已经登录，调用服务，修改状态
            Cart omsCartItem=new Cart();
            omsCartItem.setUserId(memberId);
            omsCartItem.setCommoditySkuId(skuId);
            omsCartItem.setIsChecked(isChecked);
            cartService.checkCart(omsCartItem);
            //将最新的数据从缓存中查出，渲染给购物车内嵌页
            omsCartItems=cartService.getCartListFromCache(memberId);
            //同步到缓存
            cartService.flushCartCache(memberId);
        }
        /*else{
            //没有登录,修改cookie中的数据
            String cartListCookie= CookieUtil.getCookieValue(request,"cartListCookie",true);
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItems=JSON.parseArray(cartListCookie,Cart.class);
                for (Cart cart:omsCartItems) {
                    System.out.println(cart.getCommoditySkuId()+","+cart.getIsChecked()+","+cart.getPrice()+","+cart.getTotalPrice());
                    if(cart.getCommoditySkuId().equals(skuId)){
                        cart.setIsChecked(isChecked);
                    }
                }
            }
        }*/
        /*//更新cookie
        CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        modelMap.put("cartList",omsCartItems);*/
        //购物车勾选商品的总价格
        Collections.sort(omsCartItems,new CartComparator());
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("cartList",omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartSize",omsCartItems.size());
        return "cartListInner";
    }

    private BigDecimal getTotalAmount(List<Cart> omsCartItems) {
        BigDecimal totalAmount=new BigDecimal("0");
        //存在商品才计算总价
        if(omsCartItems!=null&&omsCartItems.size()>0){
            for(Cart omsCartItem:omsCartItems){
                //先计算每个购物车的总价
                BigDecimal price=omsCartItem.getPrice().multiply(omsCartItem.getQuantity());
                if(omsCartItem.getIsChecked().equals("1")){
                    //再进行加总
                    totalAmount=totalAmount.add(price);
                }
            }
        }
        return totalAmount;
    }

    @RequestMapping("/cartList")
    @LoginRequired(loginSuccess = true)
    public String cartList(HttpServletRequest request,ModelMap modelMap){
        List<Cart> omsCartItems=new ArrayList<>();
        String memberId=(String)request.getAttribute("userId");
        String nickname=(String)request.getAttribute("username");
        System.out.println("userId:"+memberId+",username:"+nickname);
        if(StringUtils.isNotBlank(memberId)){
            //已经登录，先查缓存
            omsCartItems=cartService.getCartListFromCache(memberId);
            //如果没有数据，再查询数据库
            if(omsCartItems.size()==0){
                omsCartItems=cartService.getCartFromDb(memberId);
            }
        }
        //如果购物车存在商品
        if(omsCartItems.size()>0){
            //计算购物车每件商品的总价格
            for(Cart omsCartItem:omsCartItems){
                System.out.println("commodityName: "+omsCartItem.getCommoditySkuId() +omsCartItem.getCommodityName());
                omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
            }

        }
        Collections.sort(omsCartItems,new CartComparator());
        //购物车勾选商品的总价格
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("userId",memberId);
        modelMap.put("username",nickname);
        modelMap.put("cartList",omsCartItems);
        modelMap.put("cartSize",omsCartItems.size());
        return "cartList";
    }
    
    @RequestMapping("/addToCart")
    @LoginRequired(loginSuccess = true)
    public String addToCart(String skuId, BigDecimal quantity, HttpServletRequest request,ModelMap modelMap){
       List<Cart> omsCartItems=new ArrayList<>();
        //调用商品服务查询商品信息
        System.out.println("the skuId is:=========================="+skuId);
        CommoditySkuInfo skuInfo = skuService.getSkuInfoBySkuId(skuId);

        //将商品信息封装封装成购物车信息
        Cart omsCartItem=new Cart();
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setCommoditySaleAttr("");
        omsCartItem.setCommodityId(skuInfo.getCommodityId());
        omsCartItem.setCommodityName(skuInfo.getSkuName());
        omsCartItem.setCommodityPicture(skuInfo.getSkuDefaultImg());
        omsCartItem.setCommoditySkuId(skuId);
        omsCartItem.setQuantity(quantity);
        List<CommoditySkuSaleAttributeValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        String saleAttrStr="";
        if(skuSaleAttrValueList!=null){
            for(CommoditySkuSaleAttributeValue attributeValue:skuSaleAttrValueList){
                saleAttrStr+=attributeValue.getSaleAttrValueName()+"  ";
            }
        }else{
            System.out.println("销售属性值为空");
        }
        omsCartItem.setCommoditySaleAttr(saleAttrStr);
        omsCartItem.setIsChecked("1");
        omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));

        String memberId=(String)request.getAttribute("userId");
        /*if(StringUtils.isBlank(memberId)){
            //用户没有登录
            //获取缓存里原有的购物车数据
            String cartListCookie=;
            if(StringUtils.isBlank(cartListCookie)){
                //如果cookie为空
                omsCartItems.add(omsCartItem);
            }
            else{
                //如果cookie不为空
                omsCartItems=JSON.parseArray(cartListCookie,Cart.class);
                //判断添加的购物车数据在cookie中是否已经存在
                boolean exist=if_cart_exist(omsCartItems,omsCartItem);
                if(exist){
                    //如果已经存在，之前添加过，则更新该购物车商品数量
                    for(Cart cartItem:omsCartItems){
                        if(cartItem.getCommoditySkuId().equals(omsCartItem.getCommoditySkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                        }
                    }
                }
                else{
                    //之前没有添加过该商品，则新赠该商品到购物车
                    omsCartItems.add(omsCartItem);
                }
            }

            //更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        }*/
            //用户已登录
            //从数据库中查出购物车数据
            Cart omsCartItemFromDb=cartService.ifCartExistByUser(memberId,skuId);

            if(omsCartItemFromDb==null){
                //该用户购物车没有该商品
                omsCartItem.setUserId(memberId);
                omsCartItem.setQuantity(quantity);
                cartService.addCart(omsCartItem);
            }
            else{
                //该用户添加过该商品
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);

        modelMap.put("skuInfo",skuInfo);
        modelMap.put("skuNum",quantity);
        return "success";
    }

    @RequestMapping("deleteCart/{skuId}")
    @LoginRequired(loginSuccess = true)
    public String deleteCart(@PathVariable("skuId") String skuId,HttpServletRequest request,ModelMap modelMap){
        ModelAndView mv=new ModelAndView();
        String userId=(String)request.getAttribute("userId");
        cartService.delCart(skuId);
        List<Cart> cartList=cartService.getCartFromDb(userId);
        Collections.sort(cartList,new CartComparator());
        cartService.flushCartCache(userId);
        return "redirect:/cartList";
    }

    @RequestMapping("/cartChange")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String cartChange(int num,String cartId){
        if(num==0){
            num=1;
        }
        System.out.println("cartChange num:"+num);
        Cart cart=new Cart();
        cart.setId(cartId);
        BigDecimal quantity=new BigDecimal(num);
        cart.setQuantity(quantity);
        cartService.cartQuantityChange(cart);
        return "数量已改变";
    }

    private boolean if_cart_exist(List<Cart> omsCartItems, Cart omsCartItem) {

        boolean b = false;

        for (Cart cartItem : omsCartItems) {
            String productSkuId = cartItem.getCommoditySkuId();

            if (productSkuId.equals(omsCartItem.getCommoditySkuId())) {
                b = true;
            }
        }


        return b;
    }
}
