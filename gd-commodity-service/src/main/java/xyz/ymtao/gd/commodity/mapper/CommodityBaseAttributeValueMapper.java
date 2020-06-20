package xyz.ymtao.gd.commodity.mapper;

import tk.mybatis.mapper.common.Mapper;
import xyz.ymtao.gd.entity.CommodityBaseAttributeValue;

public interface CommodityBaseAttributeValueMapper extends Mapper<CommodityBaseAttributeValue> {
    String selectValueName(String valueId);
}
