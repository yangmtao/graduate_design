package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.entity.CommoditySkuSaleAttributeValue;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttribute;
import xyz.ymtao.gd.service.SkuService;
import xyz.ymtao.gd.service.SpuService;
import xyz.ymtao.gd.service.WmsWareSkuService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;
    @Reference
    WmsWareSkuService wareSkuService;

    @RequestMapping("/item/{skuId}")
    public String item(@PathVariable("skuId") String skuId, ModelMap modelMap){
        //获取该商品sku信息
        CommoditySkuInfo pmsSkuInfo=skuService.getSkuInfoBySkuId(skuId);
        //获取该商品的相关推荐商品信息
        List<CommoditySkuInfo> recomend=skuService.getRecomend();
        //获取该商品的库存数量
        int stock=wareSkuService.getStockBySkuId(skuId);
        modelMap.put("stock",stock);
        modelMap.put("recomend",recomend);
        modelMap.put("skuInfo",pmsSkuInfo);
        //获取该商品对应的商品spu销售属性
        List<CommoditySpuSaleAttribute> pmsProductSaleAttrs=spuService.getSpuSaleAttrListBySku(pmsSkuInfo.getCommodityId(),pmsSkuInfo.getId());
        //商品销售属性及属性值列表
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        //当前sku对应商品的其他sku销售属性值集合的hash表
        Map<String,String> skuSaleAttrHsh=new HashMap<>();
        List<CommoditySkuInfo> pmsSkuInfos = skuService.getSkuAndSaleAttrValueList(pmsSkuInfo.getCommodityId());
        for(CommoditySkuInfo skuInfo:pmsSkuInfos){
            String k="";
            String v=skuInfo.getId();
            List<CommoditySkuSaleAttributeValue> skuSaleAttrValueList=skuInfo.getSkuSaleAttrValueList();
            for(CommoditySkuSaleAttributeValue pmsSkuSaleAttrValue:skuSaleAttrValueList){
                k+=pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";
            }
            skuSaleAttrHsh.put(k,v);
        }
        //将sku的销售属性hash放到页面
        String skuSaleAttrHashJsonStr= JSON.toJSONString(skuSaleAttrHsh);
        modelMap.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);
        return "item";
    }

}
