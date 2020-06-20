package xyz.ymtao.gd.commodity;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;
import xyz.ymtao.gd.commodity.mapper.BrandMapper;
import xyz.ymtao.gd.commodity.service.SearchServiceImpl;
import xyz.ymtao.gd.entity.Brand;
import xyz.ymtao.gd.service.SearchService;
import xyz.ymtao.gd.service.util.RedisUtil;

import javax.xml.ws.soap.Addressing;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GdCommodityServiceApplicationTests {
    @Autowired
    RedisUtil redisUtil;

    @Autowired
    BrandMapper brandMapper;

    @Test
    public void contextLoads() {
        Jedis jedis=null;
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
                List<Brand> brandList=brandMapper.selectAll();
                String brandStr= JSON.toJSONString(brandList);
                jedis.set("brand",brandStr);
            }
        }catch (Exception e){
            e.printStackTrace();;
        }finally {
            jedis.close();
        }
    }

}
