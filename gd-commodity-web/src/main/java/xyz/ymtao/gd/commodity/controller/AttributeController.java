package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xyz.ymtao.gd.entity.CommodityBaseAttribute;
import xyz.ymtao.gd.entity.CommodityBaseAttributeValue;
import xyz.ymtao.gd.entity.CommodityBaseSaleAttribute;
import xyz.ymtao.gd.service.AttributeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class AttributeController {
    @Reference
    AttributeService attrService;

    @RequestMapping("/attrInfoList")
    public List<CommodityBaseAttribute> getAttrInfoList(@RequestParam("catalog3Id") String  catalog3Id){
        return attrService.getAttrInfo(catalog3Id);
    }

    @RequestMapping("/saveAttrInfo")
    public Map<String,String> saveAttrInfo(@RequestBody CommodityBaseAttribute attrInfo){
        String result=attrService.saveAttrInfo(attrInfo);
        Map<String,String> map=new HashMap<>();
        map.put("msg",result);
        return map;
    }

    @RequestMapping("/getAttrValueList")
    public List<CommodityBaseAttributeValue> getAttrInfo(@RequestParam("attrId") String attrId){
        return attrService.getAttrValueList(attrId);
    }

    @RequestMapping("/baseSaleAttrList")
    public List<CommodityBaseSaleAttribute> getBaseSaleAttr(){
        return attrService.getBaseSaleAttr();
    }
}
