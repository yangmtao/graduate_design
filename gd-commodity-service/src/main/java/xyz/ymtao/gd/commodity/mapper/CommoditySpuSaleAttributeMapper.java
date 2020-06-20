package xyz.ymtao.gd.commodity.mapper;

import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttribute;

import java.util.List;

public interface CommoditySpuSaleAttributeMapper extends Mapper<CommoditySpuSaleAttribute> {
    List<CommoditySpuSaleAttribute> selectSpuSaleAttrListBySku(@Param("commodityId") String commodityId, @Param("skuId") String skuId);
}

