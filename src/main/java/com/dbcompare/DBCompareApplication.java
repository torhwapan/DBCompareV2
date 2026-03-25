package com.dbcompare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dbcompare.mapper")
public class DBCompareApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBCompareApplication.class, args);
    }

}