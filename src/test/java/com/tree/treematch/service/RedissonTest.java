package com.tree.treematch.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test(){
        //list 数据存在本地JVM中
        List<Object> list = new ArrayList<>();
        list.add("tree");
        System.out.println("list:"+list.get(0));
//        list.remove(0);

        //数据存在 Redis
        RList<Object> rList = redissonClient.getList("test-list");//key
        //rList.add("tree");
        System.out.println("rList:"+rList.get(0));
        rList.remove(0);

        //map
        Map<String, Integer> map = new HashMap<>();
        map.put("tree",18);
        map.get("tree");

        RMap<Object, Object> rMap = redissonClient.getMap("test-map");
        rMap.put("tree",22);


        //set


    }
}
