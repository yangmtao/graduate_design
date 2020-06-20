package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;
import xyz.ymtao.gd.commodity.mapper.*;
import xyz.ymtao.gd.entity.*;
import xyz.ymtao.gd.service.SkuService;
import xyz.ymtao.gd.service.util.RedisUtil;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    CommoditySkuInfoMapper skuInfoMapper;
    @Autowired
    CommoditySkuAttributeValueMapper skuAttrValueMapper;
    @Autowired
    CommoditySkuSaleAttributeValueMapper skuSaleAttrValueMapper;
    @Autowired
    CommoditySkuImageMapper skuImageMapper;
    @Autowired
    AttrCatalogMapper attrCatalogMapper;
    @Autowired
    CommodityBaseAttributeMapper baseAttributeMapper;
    @Autowired
    CommodityBaseAttributeValueMapper baseAttributeValueMapper;

    @Override
    public String saveSkuInfo(CommoditySkuInfo skuInfo) {
        try{
            // 插入skuInfo
            int i = skuInfoMapper.insertSelective(skuInfo);
            String skuId = skuInfo.getId();

            // 插入平台属性关联
            List<CommoditySkuAttributeValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
            for (CommoditySkuAttributeValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuId);
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }

            // 插入销售属性关联
            List<CommoditySkuSaleAttributeValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (CommoditySkuSaleAttributeValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }

            // 插入图片信息
            List<CommoditySkuImage> skuImageList = skuInfo.getSkuImageList();
            for (CommoditySkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
                skuImageMapper.insertSelective(skuImage);
            }
            return "success";
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public CommoditySkuInfo getSkuBySkuIdFormDB(String skuId) {
        CommoditySkuInfo pmsSkuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //获取图片列表
        CommoditySkuImage pmsSkuImage=new CommoditySkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<CommoditySkuImage> images=skuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(images);
        //获取销售属性值
        CommoditySkuSaleAttributeValue saleAttributeValue=new CommoditySkuSaleAttributeValue();
        saleAttributeValue.setSkuId(skuId);
        List<CommoditySkuSaleAttributeValue> saleAttributeValues=skuSaleAttrValueMapper.select(saleAttributeValue);
        pmsSkuInfo.setSkuSaleAttrValueList(saleAttributeValues);
        //获取属性和属性值
        CommoditySkuAttributeValue attributeValue=new CommoditySkuAttributeValue();
        attributeValue.setSkuId(skuId);
        List<CommoditySkuAttributeValue> attributeValues=skuAttrValueMapper.select(attributeValue);
        for(CommoditySkuAttributeValue value:attributeValues){
            String attr=baseAttributeMapper.selectAttrName(value.getAttrId());
            String attrValue=baseAttributeValueMapper.selectValueName(value.getValueId());
            value.setAttrName(attr);
            value.setAttrValueName(attrValue);
        }
        pmsSkuInfo.setSkuAttrValueList(attributeValues);
        return pmsSkuInfo;
    }

    @Override
    public CommoditySkuInfo getSkuInfoBySkuId(String skuId){
        CommoditySkuInfo pmsSkuInfo=new CommoditySkuInfo();
        //连接缓存
        Jedis jedis=redisUtil.getJedis();

        //查询缓存
        String skuKey="sku:"+skuId+":info";
        String skuJson=jedis.get(skuKey);
        if(StringUtils.isNotBlank(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, CommoditySkuInfo.class);
        }
        else{
            //如果缓存中没有，查询MySQL
            //设置redis分布式锁
            String token = UUID.randomUUID().toString();
            String OK=jedis.set("sku:"+skuId+":lock",token,"nx","px",10*1000);
            if(OK!=null&&OK.equals("OK")){
                //设置成功，有权访问数据库
                pmsSkuInfo=getSkuBySkuIdFormDB(skuId);
                if(pmsSkuInfo!=null){
                    //MySQL将查询数据放入缓存
                    jedis.set(skuKey,JSON.toJSONString(pmsSkuInfo));
                }
                else{
                    //数据库中也不存在查询数据
                    //为了防止缓存穿透，将null或者空字符串设置给redis，并设置过期时间
                    jedis.setex(skuKey,60,JSON.toJSONString(pmsSkuInfo));
                }
                //在访问MySQL后，将锁释放
                String lock=jedis.get("sku:"+skuId+":lock");
                if(lock!=null&&lock.equals(token)){
                    //也可以用lua脚本，在查询到key的同时删除该key，防止高并发下的意外发生，jedis.eval("lua
                    jedis.del("sku:"+skuId+":locak");
                }
            }
            else{
                //设置失败，使当前线程睡眠几秒后再重新执行本方法

                try{
                    Thread.sleep(2000);
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                return getSkuInfoBySkuId(skuId);
            }



        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<CommoditySkuInfo> getSkuAndSaleAttrValueList(String commodityId) {
       List<CommoditySkuInfo> skuInfos= skuInfoMapper.selectSkuSaleAttrValueListBySpu(commodityId);
       return skuInfos;
    }

    @Override
    public List<CommoditySkuInfo> getSkuAndAttrValueList(String skuId) {

        return null;
    }

    @Override
    public List<CommoditySkuInfo> getAllSku() {
        List<CommoditySkuInfo> pmsSkuInfos = skuInfoMapper.selectAll();

        for (CommoditySkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
           //处理属性值
            CommoditySkuAttributeValue pmsSkuAttrValue = new CommoditySkuAttributeValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<CommoditySkuAttributeValue> select = skuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(select);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String commoditySkuId, BigDecimal price) {
        CommoditySkuInfo skuInfo=skuInfoMapper.selectByPrimaryKey(commoditySkuId);
        if(skuInfo!=null && skuInfo.getPrice().compareTo(price)==0){
            return true;
        }
        return false;
    }



    @Override
    public List<IndexSkuInfo> getIndexSku() {
        List<IndexSkuInfo> indexSkuInfoList=new ArrayList<>();
        //特价新品
        List<String> newSkuList=new ArrayList<>();
        newSkuList.add("112");newSkuList.add("113");newSkuList.add("118");newSkuList.add("121");newSkuList.add("124");newSkuList.add("142");
        IndexSkuInfo indexNew=getIndexSku("特价新品",newSkuList);
        indexSkuInfoList.add(indexNew);
        //热卖商品
        List<String> hotList=new ArrayList<>();
        hotList.add("130");hotList.add("142");hotList.add("118");hotList.add("151");hotList.add("157");hotList.add("128");
        IndexSkuInfo hotSku=getIndexSku("热卖商品",hotList);
        indexSkuInfoList.add(hotSku);
        //湖钓休闲
        List<String> relaxList=new ArrayList<>();
        relaxList.add("145");relaxList.add("148");relaxList.add("118");relaxList.add("129");relaxList.add("124");relaxList.add("128");
        IndexSkuInfo relaxSku=getIndexSku("湖钓休闲",relaxList);
        indexSkuInfoList.add(relaxSku);
        //台钓竞技
        List<String> contestList=new ArrayList<>();
        contestList.add("139");contestList.add("142");contestList.add("136");contestList.add("154");contestList.add("124");contestList.add("128");
        IndexSkuInfo contestSku=getIndexSku("台钓竞技",contestList);
        indexSkuInfoList.add(contestSku);

        return indexSkuInfoList;
    }

    @Override
    public List<CommoditySkuInfo> getRecomend() {
        List<String> recommendList=new ArrayList<>();
        recommendList.add("136");recommendList.add("142");recommendList.add("124");recommendList.add("151");recommendList.add("148");recommendList.add("128");
        Example example=new Example(CommoditySkuInfo.class);
        example.createCriteria().andIn("id",recommendList);
        List<CommoditySkuInfo> skuInfos=skuInfoMapper.selectByExample(example);
        return skuInfos;
    }

    @Override
    public List<Brand> getBrand() {
        List<Brand> brandList=null;
        Jedis jedis=null;
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
              String str=jedis.get("brand");
              brandList=JSON.parseArray(str,Brand.class);
            }
        }catch (Exception e){
            System.out.println("获取品牌信息时，redis出现异常");
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        return brandList;
    }

    private IndexSkuInfo getIndexSku(String title,List<String> skuIdList){
        IndexSkuInfo indexSkuInfo=new IndexSkuInfo();
        Example example=new Example(CommoditySkuInfo.class);
        example.createCriteria().andIn("id",skuIdList);
        List<CommoditySkuInfo> commoditySkuInfos=skuInfoMapper.selectByExample(example);
        indexSkuInfo.setName(title);
        indexSkuInfo.setSkuInfos(commoditySkuInfos);
        return indexSkuInfo;
    }
}
