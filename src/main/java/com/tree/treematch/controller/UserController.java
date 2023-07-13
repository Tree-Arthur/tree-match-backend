package com.tree.treematch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tree.treematch.common.BaseResponse;
import com.tree.treematch.common.ErrorCode;
import com.tree.treematch.common.ResultUtils;
import com.tree.treematch.exception.BusinessException;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.model.request.UserLoginRequest;
import com.tree.treematch.model.request.UserRegisterRequest;
import com.tree.treematch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tree.treematch.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 * @author tree
 * "http://localhost:5173",
 */
@Slf4j
@RestController
@RequestMapping("/user")
//@CrossOrigin(origins = "http://43.138.192.49", allowCredentials = "true")
//@CrossOrigin(origins = {"http://localhost:5173"}, allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String plantCode = userRegisterRequest.getPlanetCode();
        //校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, plantCode)) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, plantCode);
//        return new BaseResponse<>(0,result,"ok");
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
//            return null;
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        //校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
//            return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
//        return new BaseResponse<User>(0,user,"ok");
        return ResultUtils.success(user);
    }

    /**
     * 获取当前用户信息
     *
     * @param request
     * @return JSESSIONID=57EFF7DF986F47B6366DB83052458DD6; JSESSIONID=E79AF779B88776FA4925A5FB3BFE941E
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        //TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
//            return null;
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据用户名查询
     *
     * @param username 用户名
     * @return 用户脱敏
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUser(String username, HttpServletRequest request) {
        //鉴权仅管理员可查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);

        }
        //过滤
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> {
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    //todo 推荐未实现

    /**
     * 推荐
     *
     * @param pageSize 一页多少数据
     * @param pageNum  数据下标
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();//定义Redis数据结构为String泛型
        //如果有缓存,直接读缓存
        String redisKey = String.format("yupao:user:recommend:%s", loginUser.getId());
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        //无缓存,查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();//构造查询条件
        //下标,每页多少条数据
        userPage = userService.page(new Page<>(
                pageNum, pageSize), queryWrapper);//查询所有数据
        //写缓存
        try {
            valueOperations.set(redisKey, userPage, 60000, TimeUnit.MILLISECONDS);//设置毫秒级1分钟过期
        } catch (Exception e) {
            log.error("redis set error" + e);
        }
        return ResultUtils.success(userPage);
    }

    /**
     * 通过标签搜索用户
     *
     * @param tagNameList 标签列表
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        //校验
        if (CollectionUtils.isEmpty(tagNameList)) {//如果tagNameList为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR);//抛出异常
        }
        //根据标签查询用户服务返回用户列表
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 修改用户信息
     *
     * @param user    用户
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        //1 校验参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        //2 校验权限(userService校验)
        //3 更新用户信息
        Integer result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据用户名删除
     *
     * @param id 用户id
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);//逻辑删除
        return ResultUtils.success(b);
    }

    /**
     * 获取最匹配的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }
}
