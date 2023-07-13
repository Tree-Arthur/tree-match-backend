package com.tree.treematch.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {

    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        // 1. Create config object 创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s",host,port);//redis地址
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);//设置地址及第几个数据库
        // 2. Create Redisson instance 创建实例
        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
