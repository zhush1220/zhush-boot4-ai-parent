package com.zhush.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhush.**.mapper")
public class ZhushServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhushServerApplication.class, args);
    }

}
