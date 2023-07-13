package com.tree.treematch.job;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 */
@Slf4j
@Component
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;

    //重点用户(内部人员)
    private List<Long> mainUserList = Arrays.asList(1L);
    //每天执行,预热推荐用户
    @Scheduled(cron = "0 5 0 * * *")//秒 分 时
    public void doCacheRecommendUser(){
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");//key
        try {
            //只有一个线程能获取锁
            //等待时间为0(今天拿不到就第二天不等待) 释放时间(过期时间)
            if (lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){//尝试获取锁(抢锁)
//                Thread.sleep(30000);//线程睡眠 延长执行时间
                System.out.println("getLock: " + Thread.currentThread().getId());//得到锁

                for (Long userId: mainUserList) {
                    //查数据库
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();//构造查询条件
                    //下标,每页多少条数据
                    Page<User> userPage = userService.page(new Page<>(1,20),queryWrapper);//查询所有数据

                    String redisKey = String.format("yupao:user:recommend:%s",userId);
                    ValueOperations<String,Object> valueOperations = redisTemplate.opsForValue();//定义Redis数据结构为String泛型
                    //写缓存
                    try {
                        valueOperations.set(redisKey,userPage,60000, TimeUnit.MILLISECONDS);//设置毫秒级1分钟过期
                    } catch (Exception e) {
                        log.error("redis set error" + e);
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }finally {//避免try报错无法释放锁
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()){
                System.out.println("unLock: " + Thread.currentThread().getId());//释放锁
                lock.unlock();
            }
        }
    }
}
