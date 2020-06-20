package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.ymtao.gd.commodity.mapper.CommodityBaseAttributeMapper;
import xyz.ymtao.gd.commodity.mapper.CommodityBaseAttributeValueMapper;
import xyz.ymtao.gd.commodity.mapper.CommodityBaseSaleAttributeMapper;
import xyz.ymtao.gd.entity.CommodityBaseAttribute;
import xyz.ymtao.gd.entity.CommodityBaseAttributeValue;
import xyz.ymtao.gd.entity.CommodityBaseSaleAttribute;
import xyz.ymtao.gd.service.AttributeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AttributeServiceImpl implements AttributeService {
    @Autowired
    CommodityBaseAttributeMapper attrInfoMapper;
    @Autowired
    CommodityBaseAttributeValueMapper attrValueMapper;
    @Autowired
    CommodityBaseSaleAttributeMapper baseSaleAttrMapper;

    @Override
    public List<CommodityBaseAttribute> getAttrInfo(String  catalog3Id) {
        CommodityBaseAttribute pmsBaseAttrInfo = new CommodityBaseAttribute();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<CommodityBaseAttribute> pmsBaseAttrInfos = attrInfoMapper.select(pmsBaseAttrInfo);
        for (CommodityBaseAttribute baseAttrInfo : pmsBaseAttrInfos) {

            List<CommodityBaseAttributeValue> pmsBaseAttrValues = new ArrayList<>();
            CommodityBaseAttributeValue pmsBaseAttrValue = new CommodityBaseAttributeValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());
            pmsBaseAttrValues = attrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValues);
        }

        return pmsBaseAttrInfos;
    }

    @Override
    public String saveAttrInfo(CommodityBaseAttribute attrInfo) {
        String id=attrInfo.getId();
        List<CommodityBaseAttributeValue> valueList=attrInfo.getAttrValueList();
        int v=0;
        if(id==null){
            //执行插入数据
            attrInfoMapper.insertSelective(attrInfo);
            for (CommodityBaseAttributeValue value:valueList) {
                value.setAttrId(attrInfo.getId());
                v=attrValueMapper.insertSelective(value);
                if(v!=1){
                    return "fail,属性值写入错误";
                }
            }
        }
        else{
            //执行修改数据
            attrInfoMapper.updateByPrimaryKeySelective(attrInfo);
            //先删除旧属性值，
            CommodityBaseAttributeValue attrValue=new CommodityBaseAttributeValue();
            attrValue.setAttrId(id);
            attrValueMapper.delete(attrValue);
            //再插入新值
            for (CommodityBaseAttributeValue value:valueList) {
                v=attrValueMapper.insertSelective(value);
                if(v!=1){
                    return "fail,属性值写入错误";
                }
            }

        }

        return "success";
    }

    @Override
    public List<CommodityBaseAttributeValue> getAttrValueList(String attrId) {
        CommodityBaseAttributeValue attrValue=new CommodityBaseAttributeValue();
        attrValue.setAttrId(attrId);
        return attrValueMapper.select(attrValue);
    }

    @Override
    public List<CommodityBaseSaleAttribute> getBaseSaleAttr() {

        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public List<CommodityBaseAttribute> getAttrValueListByValueId(Set<String> valueIdSet) {
        String valueIdStr= StringUtils.join(valueIdSet,",");
        List<CommodityBaseAttribute> pmsBaseAttrInfos=attrInfoMapper.selectAttrValueListByValueId(valueIdStr);
        return pmsBaseAttrInfos;
    }
}

