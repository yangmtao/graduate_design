package xyz.ymtao.gd.commodity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ThymeleafConfig {

    @Autowired
    @Qualifier("thymeleafViewResolver")
    public void myViewConfig(ThymeleafViewResolver thymeleafViewResolver){
        if(thymeleafViewResolver!=null){
            Map<String,Object> map=new HashMap<>();
            map.put("sys_name","渔具阁");
        }
    }
}
