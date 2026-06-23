package com.campus.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.campus.management.mapper")
@SpringBootApplication
public class CampusActivityManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusActivityManagementApplication.class, args);
    }
}
