package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import xyz.ymtao.gd.annotation.LoginRequired;
import xyz.ymtao.gd.entity.*;
import xyz.ymtao.gd.service.AttributeService;
import xyz.ymtao.gd.service.SearchService;
import xyz.ymtao.gd.service.SkuService;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    SkuService skuService;

    @Reference
    AttributeService attrService;

    //执行搜索
    @RequestMapping("list.html")
    public String list(SearchParam pmsSearchParam, ModelMap modelMap){// 三级分类id、关键字、

        if(pmsSearchParam.getKeyword()==null&&pmsSearchParam.getCatalog3Id()==null&&pmsSearchParam.getValueId()==null){
            System.out.println("无效搜索："+pmsSearchParam.getKeyword());
            return "error/404";
        }
        // 调用搜索服务，返回搜索结果
        List<SearchSkuInfo> pmsSearchSkuInfos =  searchService.list(pmsSearchParam);
        if(pmsSearchSkuInfos.size()==0){
            modelMap.put("skuLsInfoList",null);
            return "search_list";
        }
        modelMap.put("skuLsInfoList",pmsSearchSkuInfos);

        //抽取检索结果所包含的平台属性集合
        Set<String> valueIdSet=new HashSet<>();
        for(SearchSkuInfo pmsSearchSkuInfo:pmsSearchSkuInfos){
            List<CommoditySkuAttributeValue> skuAttrValueList=pmsSearchSkuInfo.getSkuAttrValueList();
            for(CommoditySkuAttributeValue pmsSkuAttrValue:skuAttrValueList){

                String valueId=pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        //根据valueId将属性列表查询出来
        List<CommodityBaseAttribute> pmsBaseAttrInfos=attrService.getAttrValueListByValueId(valueIdSet);
        modelMap.put("attrList",pmsBaseAttrInfos);

        //对平台属性集合进一步处理，去掉当前valueId所在的属性组
        String[] delValueIds=pmsSearchParam.getValueId();
        if(delValueIds!=null){
           //面包屑
           List<SearchCrumb> pmsSearchCrumbs=new ArrayList<>();
           for(String delValueId:delValueIds){
               Iterator<CommodityBaseAttribute> iterator=pmsBaseAttrInfos.iterator();
               SearchCrumb pmsSearchCrumb=new SearchCrumb();
               pmsSearchCrumb.setValueId(delValueId);
               pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam,delValueId));
               while(iterator.hasNext()){
                   CommodityBaseAttribute pmsBaseAttrInfo=iterator.next();
                   List<CommodityBaseAttributeValue> attrValueList=pmsBaseAttrInfo.getAttrValueList();
                   for(CommodityBaseAttributeValue pmsBaseAttrValue:attrValueList){
                       String valueId=pmsBaseAttrValue.getId();
                       if(delValueId.equals(valueId)){
                           //查找面包屑的属性值名称
                           pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                           //删除该属性值所在的属性组
                           iterator.remove();
                       }
                   }
               }
               pmsSearchCrumbs.add(pmsSearchCrumb);
           }
           modelMap.put("attrValueSelectedList",pmsSearchCrumbs);
        }
        
        
        String urlParam=getUrlParam(pmsSearchParam);
        modelMap.put("urlParam",urlParam);
        String keyword=pmsSearchParam.getKeyword();
        if(StringUtils.isNotBlank(keyword)){
            modelMap.put("keyword",keyword);
        }

        return "search_list";
    }

    private String getUrlParamForCrumb(SearchParam pmsSearchParam, String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                if (!pmsSkuAttrValue.equals(delValueId)) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                }
            }
        }

        return urlParam;
    }

    private String getUrlParam(SearchParam pmsSearchParam) {
        String keyword=pmsSearchParam.getKeyword();
        String catalog3Id=pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList=pmsSearchParam.getValueId();
        String urlParam="";
        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam=urlParam+"&";
            }
            urlParam=urlParam+"keyword"+keyword;
        }
        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam=urlParam+"&";
            }
            urlParam=urlParam+"catalog3Id"+catalog3Id;
        }
        if(skuAttrValueList!=null){
            for(String pmsSkuAttrValue:skuAttrValueList){
                urlParam=urlParam+"&valueId="+pmsSkuAttrValue;
            }
        }
        return urlParam;
    }

    @RequestMapping("/index")
    @LoginRequired(loginSuccess = false)
    public String index(ModelMap modelMap, HttpServletRequest request){
        String username=(String)request.getAttribute("username");
        List<IndexSkuInfo> indexSkuInfoList=skuService.getIndexSku();
        List<Brand> listList=skuService.getBrand();
        if(indexSkuInfoList!=null&&indexSkuInfoList.size()!=0){
            System.out.println("将首页推荐商品加入渲染模型");
            modelMap.put("indexSkus",indexSkuInfoList);
        }
        if(listList.size()!=0){
            System.out.println("将品牌信息加入渲染模型");
            modelMap.put("brands",listList);
        }
        modelMap.put("username",username);
        return "index";
    }
}
