package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.entity.SearchParam;
import xyz.ymtao.gd.entity.SearchSkuInfo;

import java.util.List;

public interface SearchService {
    List<SearchSkuInfo> list(SearchParam searchParam);

    int saveSkuToEleastic(List<CommoditySkuInfo> skuList);
}
