package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.ymtao.gd.commodity.mapper.WmsWareSkuMapper;
import xyz.ymtao.gd.service.WmsWareSkuService;

@Service
public class WmsWareSkuServiceImpl implements WmsWareSkuService {

    @Autowired
    WmsWareSkuMapper wareSkuMapper;
    @Override
    public int getStockBySkuId(String skuId) {
        int stock=wareSkuMapper.selectStockBySkuId(Long.parseLong(skuId));
        return stock;
    }
}
