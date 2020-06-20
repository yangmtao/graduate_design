package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.ymtao.gd.commodity.mapper.CommoditySpuImageMapper;
import xyz.ymtao.gd.commodity.mapper.CommoditySpuInfoMapper;
import xyz.ymtao.gd.commodity.mapper.CommoditySpuSaleAttributeMapper;
import xyz.ymtao.gd.commodity.mapper.CommoditySpuSaleAttributeValueMapper;
import xyz.ymtao.gd.entity.CommoditySpuImage;
import xyz.ymtao.gd.entity.CommoditySpuInfo;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttribute;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttributeValue;
import xyz.ymtao.gd.service.SpuService;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    CommoditySpuInfoMapper commoditySpuInfoMapper;
    @Autowired
    CommoditySpuImageMapper commoditySpuImageMapper;
    @Autowired
    CommoditySpuSaleAttributeMapper commoditySpuSaleAttributeMapper;
    @Autowired
    CommoditySpuSaleAttributeValueMapper commoditySpuSaleAttributeValueMapper;

    //根据三级分类id获取spu商品信息
    @Override
    public List<CommoditySpuInfo> getSpuList(String catalog3Id){
        CommoditySpuInfo commoditySpuInfo=new CommoditySpuInfo();
        commoditySpuInfo.setCatalog3Id(catalog3Id);
        List<CommoditySpuInfo> commoditySpuInfoList=commoditySpuInfoMapper.select(commoditySpuInfo);
        return commoditySpuInfoList;
    }

    //保存spu商品信息
    @Override
    public String saveSpuInfo(CommoditySpuInfo spuInfo) {

        try{
            //保存商品信息
            commoditySpuInfoMapper.insertSelective(spuInfo);
            //获取返回的主键
            Integer spuId=Integer.valueOf(spuInfo.getId());
            System.out.println("新增商品spuid"+spuId);
            if(spuId!=null){
                //保存商品图片信息
                List<CommoditySpuImage> spuImageList=spuInfo.getSpuImageList();
                for(CommoditySpuImage spuImage:spuImageList){
                    spuImage.setCommodityId(spuId);
                    commoditySpuImageMapper.insertSelective(spuImage);
                }
                //保存销售属性信息
                List<CommoditySpuSaleAttribute> spuSaleAttributeList=spuInfo.getSpuSaleAttrList();
                for(CommoditySpuSaleAttribute spuSaleAttribute:spuSaleAttributeList){
                    spuSaleAttribute.setCommodityId(spuId);
                    commoditySpuSaleAttributeMapper.insertSelective(spuSaleAttribute);
                    //保存销售属性值
                    List<CommoditySpuSaleAttributeValue> spuSaleAttributeValueList=spuSaleAttribute.getSpuSaleAttrValueList();
                    for(CommoditySpuSaleAttributeValue spuSaleAttributeValue:spuSaleAttributeValueList){
                        spuSaleAttributeValue.setCommodityId(spuId);
                        commoditySpuSaleAttributeValueMapper.insertSelective(spuSaleAttributeValue);
                    }
                }
                return "success";
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //获取商品销售属性及其销售属性值
    @Override
    public List<CommoditySpuSaleAttribute> getSpuSaleAttrList(String spuId) {
        Integer spuId2=Integer.valueOf(spuId);
        CommoditySpuSaleAttribute commoditySpuSaleAttribute=new CommoditySpuSaleAttribute();
        commoditySpuSaleAttribute.setCommodityId(spuId2);
        List<CommoditySpuSaleAttribute> spuSaleAttributeList=null;
        spuSaleAttributeList=  commoditySpuSaleAttributeMapper.select(commoditySpuSaleAttribute);
        for(CommoditySpuSaleAttribute spuSaleAttribute:spuSaleAttributeList){
            CommoditySpuSaleAttributeValue spuSaleAttributeValue=new CommoditySpuSaleAttributeValue();
            spuSaleAttributeValue.setCommodityId(spuId2);
            spuSaleAttributeValue.setSaleAttrId(spuSaleAttribute.getSaleAttrId());
            List<CommoditySpuSaleAttributeValue> spuSaleAttributeValueList=commoditySpuSaleAttributeValueMapper.select(spuSaleAttributeValue);
            spuSaleAttribute.setSpuSaleAttrValueList(spuSaleAttributeValueList);
        }

            return spuSaleAttributeList;

    }

    //获取spu商品图片
    @Override
    public List<CommoditySpuImage> getSpuImageList(String spuId) {
        Integer spuId2=Integer.valueOf(spuId);
        CommoditySpuImage commoditySpuImage=new CommoditySpuImage();
        commoditySpuImage.setCommodityId(spuId2);
        List<CommoditySpuImage> spuImageList=null;
        spuImageList=commoditySpuImageMapper.select(commoditySpuImage);
        return spuImageList;
    }

    //根据商品skuid获取商品销售属性及其销售属性值
    @Override
    public List<CommoditySpuSaleAttribute> getSpuSaleAttrListBySku(String spuId, String skuId) {
        List<CommoditySpuSaleAttribute> spuSaleAttributeList = commoditySpuSaleAttributeMapper.selectSpuSaleAttrListBySku(spuId, skuId);
        return spuSaleAttributeList;
    }
}
