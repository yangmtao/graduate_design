package xyz.ymtao.gd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan("xyz.ymtao.gd.user.mapper")
@SpringBootApplication
public class GdUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GdUserServiceApplication.class, args);
    }

}
