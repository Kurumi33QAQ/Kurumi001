package com.zsj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.mybatis.spring.annotation.MapperScan;


@MapperScan("com.zsj.modules.ums.mapper")
@SpringBootTest
class MyMallPlatformApplicationTests {

    @Test
    void contextLoads() {
    }

}
