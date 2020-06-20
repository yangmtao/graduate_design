package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.Cart;

import javax.persistence.Id;
import java.util.List;

public interface CartService {
    List<Cart> getCartListFromCache(String userId);
    void checkCart(Cart cart);

    Cart ifCartExistByUser(String userId, String skuId);

    int addCart(Cart cart);

    int updateCart(Cart cartFromDb);

    void flushCartCache(String userId);

    List<Cart> mergeCart(String userId,List<Cart> cookieCart);

    void delCart(String skuId);

    List<Cart> getCartFromDb(String memberId);

    int cartQuantityChange(Cart cart);

}
