package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.ymtao.gd.entity.CommodityFirstCatalog;
import xyz.ymtao.gd.entity.CommoditySecondCatalog;
import xyz.ymtao.gd.entity.CommodityThirdCatalog;
import xyz.ymtao.gd.service.CatalogService;

import java.util.List;
@Controller
@CrossOrigin
public class CatalogController {
    @Reference
    CatalogService catalogService;

    @RequestMapping("/getCatalog1")
    @ResponseBody
    public List<CommodityFirstCatalog> getCatalog1(){
        return catalogService.getCatalog1();
    }

    @RequestMapping("/getCatalog2")
    @ResponseBody
    public List<CommoditySecondCatalog> getCatalog2(int catalog1Id){
        return catalogService.getCatalog2(catalog1Id);
    }

    @RequestMapping("/getCatalog3")
    @ResponseBody
    public List<CommodityThirdCatalog> getCatalog3(@RequestParam("catalog2Id") Integer catalog2Id){
        return catalogService.getCatalog3(catalog2Id);
    }
}
