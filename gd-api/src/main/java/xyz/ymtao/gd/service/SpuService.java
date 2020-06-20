package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.CommoditySpuImage;
import xyz.ymtao.gd.entity.CommoditySpuInfo;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttribute;

import java.util.List;

public interface SpuService {
    List<CommoditySpuInfo> getSpuList(String catalog3Id);
    String saveSpuInfo(CommoditySpuInfo spuInfo);
    List<CommoditySpuSaleAttribute> getSpuSaleAttrList(String spuId);
    List<CommoditySpuImage> getSpuImageList(String spuId);
    List<CommoditySpuSaleAttribute> getSpuSaleAttrListBySku(String spuId,String skuId);
}
