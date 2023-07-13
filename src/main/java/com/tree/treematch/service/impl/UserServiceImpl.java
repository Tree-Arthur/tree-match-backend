package com.tree.treematch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tree.treematch.common.ErrorCode;
import com.tree.treematch.exception.BusinessException;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.service.UserService;
import com.tree.treematch.mapper.UserMapper;
import com.tree.treematch.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tree.treematch.contant.UserConstant.ADMIN_ROLE;
import static com.tree.treematch.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
* @author tree
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-02-12 05:21:31
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;
    /**
     * SALT 盐值:混淆密码
     */
    public static final String SALT="tree";

    /**
     * 用户注册
     * @param userAccount  用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String plantCode) {
        //1、校验
        //非空
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,plantCode)){
            //todo 修改为自定义异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");

        }
        //账户长度 不小于 4 位
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");

        }
        //密码就 不小于 8 位吧
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");

        }
        //编号不大于5位
        if (plantCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编号不应超过5位数");
        }
        //账户不能包含特殊字符
        //String validPattern = "\\pP|\\pS|\\s+";
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            return -1;
        }
        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码和校验密码相同");

        }
        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");

        }

        //编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode",plantCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编号重复");
        }

        //2、加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3、插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(plantCode);
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"保存错误");

        }
        //返回用户ID数据
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request 请求
     * @return
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1、校验
        //非空
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            return null;
        }
        //账户长度 不小于 4 位
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户长度过短");

        }
        //密码就 不小于 8 位吧
        if (userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码过短");
        }
        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            return null;
        }
        //2、加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",/*userPassword*/ encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user == null){
            log.info("user login failed,user Account cannot match userPassword");//日志
            return null;
        }
        //3、用户脱敏
        User safetyUser = getSafetyUser(user);
        //4、记录用户登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser){
        if (originUser == null){
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUserName(originUser.getUserName());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 用户注销
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 是否为管理员
     * @param request 请求
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request){
        //鉴权仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null || user.getUserRole()!= ADMIN_ROLE){
            return false;
        }
        return true;
    }

    /**
     * 是否为管理员(方法重载)
     * @param loginUser 登录用户的信息
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser){
        //鉴权仅管理员可查询
        return loginUser != null && loginUser.getUserRole()!= ADMIN_ROLE;
    }

    /**
     * 根据标签搜索用户(内存过滤)
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //内存查询
        //1 先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //空查 清空数据库连接时间
        userMapper.selectCount(null);
        long startTime = System.currentTimeMillis();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2 在内存中判断是否包含要求的标签 并行流parallelStream()
        return userList.stream().filter((user) -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)){
                return false;
            }
            //使用gson.fromJson把这个 tagsStr JSON字符串反序列化成对象
            Set<String> temptagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType());
            //java8特性是可选类 判空简化if分支复杂度 链式调用使用ofNullable封装可能为空的对象然后使用orElse给对象一个默认值new HashSet<>()
            temptagNameSet = Optional.ofNullable(temptagNameSet).orElse(new HashSet<>());
            //对象序列化JSON字符串
//            gson.toJson(temptagNameList);
            for (String tagName : tagNameList) {
                //如果不存在任何标签返回false会过滤掉
                if (!temptagNameSet.contains(tagName)) {
                    return false;
                }
            }
            log.info("memory query time = "+ (System.currentTimeMillis() - startTime));
            return true;//保留
        }).map(this::getSafetyUser).collect(Collectors.toList());
//       return userList;
    }


    /**
     * 根据标签搜索用户(SQL查询)
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Deprecated
    public List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //sql 查询
        //开始时间
        long startTime = System.currentTimeMillis();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接 and 查询
        //like '%Java%' and like '%Python%'
        for (String tagName: tagNameList) {
            queryWrapper = queryWrapper.like("tags",tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        log.info("sql query time = "+ (System.currentTimeMillis() - startTime));
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }


    /**
     * 更新用户信息
     * @param user 需要更新的用户信息
     * @param loginUser 登录用户信息
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (userId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //todo 补充校验,如果用户没有传任何要更新的值,直接报错,不用执行update语句
        //如果是管理员,允许更新任意用户
        //如果不是管理员,只允许更新当前自己的信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()){
            //如果不是管理员 并且 需要修改的用户信息与当前登录的用户信息不相等
            throw new BusinessException(ErrorCode.NO_AUTH);//无权限
        }
        //根据id查询用户
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null){
            return null;
        }
        //从session域获取用户信息
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);

        if (userObj == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 最远距离算法匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();//查询构造器
        queryWrapper.select("id","tags");//查询的字段
        queryWrapper.isNotNull("tags");//tags不为空的
        List<User> userList = this.list(queryWrapper);//获取所有用户
        String tags = loginUser.getTags();
        Gson gson = new Gson();//
        List<String> tagList = gson.fromJson(tags,new TypeToken<List<String>>(){
        }.getType());//登录用户标签转化Json列表
        //用户列表下标 => 相似度

        List<Pair<User,Long>> list = new ArrayList<>();
        //依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);//得到当前遍历的用户
            String userTags = user.getTags();////得到当前遍历用户的标签
            //无标签或为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()){
                continue;//匹配下一个用户
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());//数据库用户标签 转化 Json列表
            //根据算法得到分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user,distance));
        }
        //按编辑距离进行由小到大排序 取前num个用户
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //把topUserPairList列表取出符合要求的Key组成新数组userVOList
        //原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userIdList);
        //1,3,2
        //user1 user2 user3
        //1 => user1 ; 2 => user2 ; 3 => user3(映射)
        Map<Long,List<User>> userIdUserListMap = this.list(userQueryWrapper).stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        ArrayList<User> finalUserList = new ArrayList<>();
        for (Long userId: userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }
}




