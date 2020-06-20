package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.CommodityBaseAttribute;
import xyz.ymtao.gd.entity.CommodityBaseAttributeValue;
import xyz.ymtao.gd.entity.CommodityBaseSaleAttribute;

import java.util.List;
import java.util.Set;

public interface AttributeService {
    public List<CommodityBaseAttribute> getAttrInfo(String catalog3Id);
    public String saveAttrInfo(CommodityBaseAttribute baseAttributeInfo);
    List<CommodityBaseAttributeValue> getAttrValueList(String attrId);
    List<CommodityBaseSaleAttribute> getBaseSaleAttr();

    List<CommodityBaseAttribute> getAttrValueListByValueId(Set<String> valueIdSet);
}
