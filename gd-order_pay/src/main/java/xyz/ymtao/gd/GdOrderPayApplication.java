package xyz.ymtao.gd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan("xyz.ymtao.gd.order_pay.mapper")
@SpringBootApplication
public class GdOrderPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GdOrderPayApplication.class, args);
    }

}
