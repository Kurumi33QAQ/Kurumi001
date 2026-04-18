package com.zsj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.zsj.modules.**.mapper")
public class MyMallPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyMallPlatformApplication.class, args);
    }

}
