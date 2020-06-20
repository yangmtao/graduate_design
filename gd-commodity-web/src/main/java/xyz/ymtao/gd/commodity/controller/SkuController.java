package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.service.SkuService;

@Controller
@CrossOrigin
public class SkuController {
    @Reference
    SkuService skuService;

    @RequestMapping("/saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody CommoditySkuInfo skuInfo){
        skuInfo.setCommodityId(skuInfo.getSpuId());
        String defaultImg=skuInfo.getSkuDefaultImg();
        if(StringUtils.isEmpty(defaultImg)){
            skuInfo.setSkuDefaultImg(skuInfo.getSkuImageList().get(0).getImgUrl());
        }

        String msg=skuService.saveSkuInfo(skuInfo);
       return msg;
    }
}
