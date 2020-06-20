package xyz.ymtao.gd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan("xyz/ymtao/gd/commodity/mapper")
@SpringBootApplication
public class GdCommodityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GdCommodityServiceApplication.class, args);
    }

}
