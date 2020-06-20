package xyz.ymtao.gd.comparator;

import xyz.ymtao.gd.entity.Cart;

import java.util.Comparator;

public class CartComparator implements Comparator<Cart> {
    @Override
    public int compare(Cart o1, Cart o2) {
        int o1Id=Integer.parseInt(o1.getId());
        int o2Id=Integer.parseInt(o2.getId());
        if(o1Id<o2Id){
            return 1;
        }
        else if(o1Id>o2Id){
            return -1;
        }
        return 0;
    }
}
