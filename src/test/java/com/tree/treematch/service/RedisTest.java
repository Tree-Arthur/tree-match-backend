package com.tree.treematch.service;

import com.tree.treematch.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增加
        valueOperations.set("treeString","dog");
        valueOperations.set("treeInt",1);
        valueOperations.set("treeDouble",2.0);
        User user = new User();
        user.setId(1L);
        user.setUserName("tree");
        valueOperations.set("treeUser",user);
        //查
        Object treeString = valueOperations.get("treeString");
        Assertions.assertTrue("dog".equals((String)treeString));

        Object treeInt = valueOperations.get("treeInt");
        Assertions.assertTrue(1 == (Integer) treeInt);
        Object treeDouble = valueOperations.get("treeDouble");
        Assertions.assertTrue(2.0 == (Double)treeDouble);
        Object treeUser = valueOperations.get("treeUser");
        System.out.println(treeUser);
    }
}
