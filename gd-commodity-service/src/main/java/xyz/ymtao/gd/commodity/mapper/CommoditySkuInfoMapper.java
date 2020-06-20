package xyz.ymtao.gd.commodity.mapper;

import tk.mybatis.mapper.common.Mapper;
import xyz.ymtao.gd.entity.CommoditySkuInfo;

import java.util.List;

public interface CommoditySkuInfoMapper extends Mapper<CommoditySkuInfo> {
    List<CommoditySkuInfo> selectSkuSaleAttrValueListBySpu(String commodityId);
}
