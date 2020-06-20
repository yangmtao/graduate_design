package xyz.ymtao.gd.entity;

import java.io.Serializable;
import java.util.List;

public class IndexSkuInfo implements Serializable {
    private String name;
    private List<CommoditySkuInfo> skuInfos;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CommoditySkuInfo> getSkuInfos() {
        return skuInfos;
    }

    public void setSkuInfos(List<CommoditySkuInfo> skuInfos) {
        this.skuInfos = skuInfos;
    }
}
