package xyz.ymtao.gd.commodity.mapper;

import tk.mybatis.mapper.common.Mapper;
import xyz.ymtao.gd.entity.WmsWareSku;

public interface WmsWareSkuMapper extends Mapper<WmsWareSku> {
    int selectStockBySkuId(Long skuId);
}
