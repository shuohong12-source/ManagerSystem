package com.example.logistics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.example.logistics.mapper")
public class SmartLogisticsApplication {
    public static void main(String[] args) {

        SpringApplication.run(SmartLogisticsApplication.class, args);
    }
}
