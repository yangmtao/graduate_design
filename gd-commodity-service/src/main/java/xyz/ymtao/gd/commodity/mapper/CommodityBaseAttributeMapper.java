package xyz.ymtao.gd.commodity.mapper;

import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;
import xyz.ymtao.gd.entity.CommodityBaseAttribute;

import java.util.List;

public interface CommodityBaseAttributeMapper extends Mapper<CommodityBaseAttribute> {
    List<CommodityBaseAttribute> selectAttrValueListByValueId(@Param("valueIdStr") String valueId);
    String selectAttrName(String attrId);
}
