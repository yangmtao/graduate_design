package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.Brand;
import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.entity.IndexSkuInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SkuService {
    String saveSkuInfo(CommoditySkuInfo skuInfo);
    CommoditySkuInfo getSkuInfoBySkuId(String skuId);
    List<CommoditySkuInfo> getSkuAndSaleAttrValueList(String commodityId);
    List<CommoditySkuInfo> getSkuAndAttrValueList(String skuId);
    List<CommoditySkuInfo> getAllSku();

    boolean checkPrice(String commoditySkuId, BigDecimal price);

    List<IndexSkuInfo> getIndexSku();
    List<CommoditySkuInfo> getRecomend();
    List<Brand> getBrand();


}
