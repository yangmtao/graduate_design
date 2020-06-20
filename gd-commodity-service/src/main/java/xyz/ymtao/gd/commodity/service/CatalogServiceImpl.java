package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;
import xyz.ymtao.gd.commodity.mapper.CommodityFirstCatalogMapper;
import xyz.ymtao.gd.commodity.mapper.CommoditySecondCatalogMapper;
import xyz.ymtao.gd.commodity.mapper.CommodityThirdCatalogMapper;
import xyz.ymtao.gd.entity.CommodityFirstCatalog;
import xyz.ymtao.gd.entity.CommoditySecondCatalog;
import xyz.ymtao.gd.entity.CommodityThirdCatalog;
import xyz.ymtao.gd.service.CatalogService;

import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {

    @Autowired
    CommodityFirstCatalogMapper catalog1Mapper;
    @Autowired
    CommoditySecondCatalogMapper catalog2Mapper;
    @Autowired
    CommodityThirdCatalogMapper catalog3Mapper;

    @Override
    public List<CommodityFirstCatalog> getCatalog1() {
        return catalog1Mapper.selectAll();
    }

    @Override
    public List<CommoditySecondCatalog> getCatalog2(Integer catalog1Id) {
        CommoditySecondCatalog secondCatalog=new CommoditySecondCatalog();
        secondCatalog.setCatalog1Id(catalog1Id);
        return catalog2Mapper.select(secondCatalog);
    }

    @Override
    public List<CommodityThirdCatalog> getCatalog3(Integer catalog2Id) {
        CommodityThirdCatalog thirdCatalog=new CommodityThirdCatalog();
        thirdCatalog.setCatalog2Id(catalog2Id);
        return catalog3Mapper.select(thirdCatalog);
    }
}
