package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import tk.mybatis.mapper.entity.Example;
import xyz.ymtao.gd.commodity.mapper.CartMapper;
import xyz.ymtao.gd.comparator.CartComparator;
import xyz.ymtao.gd.entity.Cart;
import xyz.ymtao.gd.service.CartService;
import xyz.ymtao.gd.service.util.RedisUtil;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void checkCart(Cart omsCartItem) {
        Example example=new Example(Cart.class);
        example.createCriteria().andEqualTo("userId",omsCartItem.getUserId()).andEqualTo("commoditySkuId",omsCartItem.getCommoditySkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);
    }

    @Override
    public Cart ifCartExistByUser(String memberId, String skuId) {
        Cart omsCartItem=new Cart();
        omsCartItem.setUserId(memberId);
        omsCartItem.setCommoditySkuId(skuId);
        Cart cartItem=omsCartItemMapper.selectOne(omsCartItem);
        return cartItem;
    }

    @Override
    public int addCart(Cart omsCartItem) {
        int i=0;
        if(StringUtils.isNotBlank(omsCartItem.getUserId())){
            //selective只插入非空值
            i = omsCartItemMapper.insertSelective(omsCartItem);
        }
        return i;
    }

    @Override
    public int updateCart(Cart omsCartItemFromDb) {
        Example example=new Example(Cart.class);
        example.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());
        int i = omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb, example);
        return i;
    }

    @Override
    public void flushCartCache(String memberId) {
        Cart omsCartItem=new Cart();
        omsCartItem.setUserId(memberId);
        List<Cart> omsCartItems=omsCartItemMapper.select(omsCartItem);
        Collections.sort(omsCartItems,new CartComparator());
        Jedis jedis=null;
        try{
            //将数据库中的购物车信息同步到redis缓存
            jedis=redisUtil.getJedis();
            Map<String,String> map=new HashMap<>();
            for(Cart cartItem:omsCartItems){
                cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                map.put(cartItem.getCommoditySkuId(), JSON.toJSONString(cartItem));
            }
            jedis.del("user:"+memberId+":cart");
            jedis.hmset("user:"+memberId+":cart",map);
        }catch (JedisDataException e){
            System.out.println("购物车同步到缓存失败，数据异常");
        }
        finally {
            jedis.close();
        }

    }

    @Override
    public List<Cart> getCartListFromCache(String userId) {
        Jedis jedis=null;
        List<Cart> omsCartItems=new ArrayList<>();
        try{
            jedis=redisUtil.getJedis();
            List<String> hvals=jedis.hvals("user:"+userId+":cart");
            if (hvals!=null){
                for(String hval:hvals){
                    Cart omsCartItem=JSON.parseObject(hval,Cart.class);
                    omsCartItems.add(omsCartItem);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            jedis.close();
        }
        return omsCartItems;
    }

    @Override
    public List<Cart> mergeCart(String userId,List<Cart> cookieCart){
        List<Cart> carts=new ArrayList<>();
       //查询出数据库内的购物车数据
       Example example=new Example(Cart.class);
       example.createCriteria().andEqualTo("userId",userId);
       List<Cart> cartDb=omsCartItemMapper.selectByExample(example);
       //如果数据库中没有购物车数据，直接将cookie中的购物车数据添加
       if(cartDb==null||cartDb.size()==0){
           System.out.println("数据库中无购物车数据");
           for (Cart cart:cookieCart) {
               System.out.println("同步的购物车skuid:"+cart.getCommoditySkuId());
               //cookie中的购物车数据没有用户id，因此需要加上
               cart.setUserId(userId);
               omsCartItemMapper.insertSelective(cart);
           }
           return cartDb;
       }
       else{
           //如果数据库中有购物车数据
           System.out.println("数据库中有购物车数据");
           for (Cart cartDbItem:cartDb) {
               for(Cart cartCookie:cookieCart){

                   //如果skuid不相等，说明数据库中没有此条购物车信息，需合并
                   if(!cartDbItem.getCommoditySkuId().equals(cartCookie.getCommoditySkuId())){
                       //cookie中的购物车数据没有用户id，因此需要加上
                       System.out.println("同步的购物车skuid:"+cartCookie.getCommoditySkuId());
                       cartCookie.setUserId(userId);
                       omsCartItemMapper.insertSelective(cartCookie);
                       carts.add(cartCookie);
                   }
                   else{
                       //购物车中存在此商品，检查是否一致
                       //如果不一致，更新数据
                       if(cartDbItem.getIsChecked()!=cartCookie.getIsChecked() || cartDbItem.getQuantity()!=cartCookie.getQuantity()){
                           omsCartItemMapper.updateByPrimaryKeySelective(cartCookie);
                       }
                   }
               }
           }
       }
       flushCartCache(userId);
        return carts;
    }

    //删除指定购物车
    @Override
    public void delCart(String skuId) {
        System.out.println("delCart ,the skuId is: "+skuId);
        //Example example=new Example(Cart.class);
        //example.createCriteria().andEqualTo("commodity_sku_id",skuId);
        Cart cart=new Cart();
        cart.setCommoditySkuId(skuId);
        omsCartItemMapper.delete(cart);
        //omsCartItemMapper.deleteByExample(example);
    }

    @Override
    public List<Cart> getCartFromDb(String memberId) {
        Example example=new Example(Cart.class);
        example.createCriteria().andEqualTo("userId",memberId);
        return omsCartItemMapper.selectByExample(example);
    }

    @Override
    public int cartQuantityChange(Cart cart) {
        int result=0;
        System.out.println("cartId:"+cart.getId());
        Example example=new Example(Cart.class);
        example.createCriteria().andEqualTo("id",cart.getId());
       result= omsCartItemMapper.updateByExampleSelective(cart,example);
        return result;
    }
}
