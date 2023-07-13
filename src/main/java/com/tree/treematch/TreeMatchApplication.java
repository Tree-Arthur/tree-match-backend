package com.tree.treematch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
@MapperScan("com.tree.treematch.mapper")
@EnableScheduling
public class TreeMatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(TreeMatchApplication.class, args);
    }

}