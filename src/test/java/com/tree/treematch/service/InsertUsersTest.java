package com.tree.treematch.service;

import com.tree.treematch.model.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@SpringBootTest
public class InsertUsersTest {
    @Resource
    private UserService userService;
    // 参数 默认线程数,最大线程数,线程存活时间,时间单位,任务队列(任务策略)
    private ExecutorService executorService =
            new ThreadPoolExecutor(60, 1000, 10000, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();//springboot通过统计插入数据需要多少秒工具
        stopWatch.start();//开始
        final int INSERT_NUM = 100000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUserName("假达菲鸭");
            user.setUserAccount("faketree");
            user.setAvatarUrl("https://avatars.githubusercontent.com/u/93178362?v=4");
            user.setGender(0);
            user.setProfile("");
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("11111");
            userList.add(user);

//            userMapper.insert(user);
        }
        //保存数据 用户列表每添加1000个用户保存一次
        userService.saveBatch(userList,1000);
        stopWatch.stop();//停止
        System.out.println(stopWatch.getTotalTimeMillis());//毫秒级
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();//springboot通过统计插入数据需要多少秒工具
        stopWatch.start();//开始
        final int INSERT_NUM = 100000;
        //分10组
        int batchSize = 10000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<User> userList = new ArrayList<>();
            while (true){
                j++;
                User user = new User();
                user.setUserName("假达菲鸭");
                user.setUserAccount("faketree");
                user.setAvatarUrl("https://avatars.githubusercontent.com/u/93178362?v=4");
                user.setGender(0);
                user.setProfile("");
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("11111");
                userList.add(user);
                if (j % 10000 == 0){//j取模10000等于0
                    break;//跳出 while 循环
                }
            }
            //异步执行,executorService
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                //打印输出线程名
                System.out.println("threadName: "+Thread.currentThread().getName());
                //保存数据 用户列表每添加1000个用户保存一次
                userService.saveBatch(userList, 1000);
            });
            futureList.add(future);//得到10个异步任务
        }
        //异步执行  .join阻塞直到里面任务结束才执行下面stop
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();//停止
        System.out.println(stopWatch.getTotalTimeMillis());//毫秒级
    }
}
