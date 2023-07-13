package com.tree.treematch.service;

import com.tree.treematch.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 * @author tree
 */
@SpringBootTest
class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    public void testAddUser() {
        User user = new User();
        System.out.println(user.getId());
        user.setUserName("duck");
        user.setUserAccount("huang");
        user.setAvatarUrl("D:\\Users\\Desktop\\效果图\\D5参考图\\1\\Untitled_1.21.1.jpg");
        user.setGender(0);
        user.setUserPassword("12345678");
        user.setPhone("123456");
        user.setEmail("123456");

        boolean result = userService.save(user);
        assertTrue(result);

    }
    @Test
    public void testUserRegister() {
        String userAccount = "tree";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        String planetCode = "1";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
    }

    @Test
    void userRegister() {
        String userAccount = "tree1";
        String userPassword = "";
        String checkPassword = "123456";
        String planetCode = "1";
        long result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);//断言 预期,实际

        userAccount="tr";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);//断言 预期,实际

        userAccount = "tree1";
        userPassword = "123456";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);//断言 预期,实际

        userAccount = "tr ee";
        userPassword = "12345678";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);//断言 预期,实际

        userAccount = "tree";
        checkPassword = "123456789";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);//断言 预期,实际

        userAccount = "tree1";
        userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        //Assertions.assertTrue(result > 0);//断言 预期,实际
        Assertions.assertEquals(-1,result);//断言 预期,实际

    }
}