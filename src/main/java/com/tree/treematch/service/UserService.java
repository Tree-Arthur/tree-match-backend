
package com.tree.treematch.service;

import com.tree.treematch.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
* @author tree
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2023-02-12 0[]#5:21:31
*/
public interface UserService extends IService<User> {


    /**
     * 用户注册
     * @param userAccount  用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param plantCode 编号
     * @return 新用户id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String plantCode);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request 请求
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword , HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originUser 源用户
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     * @param request
     */
    int userLogout(HttpServletRequest request);

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 根据标签搜索用户
     * @param tagNameList 标签名列表
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    @Deprecated
    List<User> searchUsersByTagsBySQL(List<String> tagNameList);

    /**
     * 更新用户信息
     *
     * @param user
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);
}
