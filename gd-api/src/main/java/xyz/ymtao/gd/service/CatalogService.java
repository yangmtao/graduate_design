package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.CommodityFirstCatalog;
import xyz.ymtao.gd.entity.CommoditySecondCatalog;
import xyz.ymtao.gd.entity.CommodityThirdCatalog;

import java.util.List;

public interface CatalogService {
    List<CommodityFirstCatalog> getCatalog1();

    List<CommoditySecondCatalog> getCatalog2(Integer catalog1Id);

    List<CommodityThirdCatalog> getCatalog3(Integer catalog2Id);
}
