package com.tree.treematch.once;

import com.tree.treematch.mapper.UserMapper;
import com.tree.treematch.model.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     * fixedDelay 间隔执行
     * fixedRate 频率
     *
     */
//    @Scheduled(initialDelay = 5000 ,fixedRate = Long.MAX_VALUE)
    private void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();//springboot通过统计插入数据需要多少秒工具
        stopWatch.start();//开始
        final int INSERT_NUM = 1000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUserName("假用户");
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

            userMapper.insert(user);
        }
        stopWatch.stop();//停止
        System.out.println(stopWatch.getTotalTimeMillis());//毫秒级
    }
}
