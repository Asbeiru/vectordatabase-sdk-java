package com.tencent.tcvectordb.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 腾讯云向量数据库 Spring Boot 应用启动类
 *
 * @author Tencent Cloud VectorDB Team
 */
@SpringBootApplication
public class VectorDBApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorDBApplication.class, args);
    }
}
