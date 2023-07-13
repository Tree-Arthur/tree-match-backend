package com.tree.treematch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tree.treematch.common.ErrorCode;
import com.tree.treematch.exception.BusinessException;
import com.tree.treematch.mapper.TeamMapper;
import com.tree.treematch.model.domain.Team;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.model.domain.UserTeam;
import com.tree.treematch.model.dto.TeamQuery;
import com.tree.treematch.model.enums.TeamStatusEnum;
import com.tree.treematch.model.request.TeamJoinRequest;
import com.tree.treematch.model.request.TeamQuitRequest;
import com.tree.treematch.model.request.TeamUpdateRequest;
import com.tree.treematch.model.vo.TeamUserVO;
import com.tree.treematch.model.vo.UserVO;
import com.tree.treematch.service.TeamService;
import com.tree.treematch.service.UserService;
import com.tree.treematch.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author tree
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-05-16 23:16:08
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取某队伍当前人数
     * @param teamId 队伍id
     * @return
     */
    private long countTeamUserByTeamId(long teamId){
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();//复用条件构造器
        userTeamQueryWrapper.eq("teamId",teamId);//相等条件
        return userTeamService.count(userTeamQueryWrapper);
    }

    /**
     * 根据id获取队伍信息
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0){//避免缓存穿透(用户请求数据库不存在的数据)
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);//通过队伍id得到队伍完整数据
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //1. 请求参数是否为空？
        if (team == null){
              throw new BusinessException(ErrorCode.PARAMS_ERROR);
          }
        //2. 是否登录，未登录不允许创建
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        //3. 校验信息
        //  a. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);//如果为空设置,默认值为0
        if (maxNum < 1 || maxNum > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不满足要求");
        }
        //  b. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍标题不满足要求");
        }
        //  c. 描述 <= 512
        String description = team.getDescription();
        //如果描述不存在并且长度大于512
        if (StringUtils.isNotBlank(description) && description.length() > 512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍描述过长");
        }
        //  d. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍添加有误");
        }
        //  e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (statusEnum.equals(TeamStatusEnum.SECRET)){
            if (StringUtils.isBlank(password)||password.length() > 32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码设置有误");
            }
        }

        //  f. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超时时间 > 当前时间");
        }
        //  g. 校验用户最多创建 5 个队伍
        //todo 可能同时添加多个队伍(加锁)
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多创建 5 个队伍");
        }

        //4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId  == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }

        //5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());//设置添加时间
        result = userTeamService.save(userTeam);//保存
        if (!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //组合查询条件
        if (teamQuery != null){
            //1. 从请求参数中取出队伍名称等查询条件，如果存在则作为查询条件
            Long id = teamQuery.getId();
            if (id != null && id >0){
                queryWrapper.eq("id",id);
            }
            List<Long> idList = teamQuery.getIdList();//获取idlist
            if (CollectionUtils.isNotEmpty(idList)){//idlist不为空
                queryWrapper.in("id",idList);//将idlist作为查询条件
            }
            //3. 可以通过某个关键词同时对名称和描述查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)){//如果名称存在且不是空字符串
                queryWrapper.and(qw -> qw.like("name",searchText).or()
                                        .like("description",searchText));
            }
            // 通过名称查询
            String name = teamQuery.getName();//名称
            if (StringUtils.isNotBlank(name)){//如果名称存在且不是空字符串
                queryWrapper.like("name",name);//模糊查询
            }
            // 通过描述查询
            String description = teamQuery.getDescription();//描述
            if (StringUtils.isNotBlank(description)){//如果名称存在且不是空字符串
                queryWrapper.like("description",description);
            }
            //查询最大人数相等的
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum >0){
                queryWrapper.eq("maxNum",maxNum);//"数据库中的值",要查的值
            }
            //根据创建者用户Id查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId >0){
                queryWrapper.eq("userId",userId);//"数据库中的值",要查的值
            }
            //根据状态来查询
            //4. 只有管理员才能查看加密还有非公开的房间
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null){//状态为空
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)){//如果不是管理并且是私密状态
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            if (status != null && status > -1){
                queryWrapper.eq("status",status);
            }
            queryWrapper.eq("status",statusEnum.getValue());
        }
        //2. 不展示已过期的队伍（根据过期时间筛选）
        //SQL  and  expireTime > now() or expireTime is null
        queryWrapper.and(qw -> qw.gt("expireTime",new Date()).or()
                                    .isNull("expireTime"));//qw子查询

        List<Team> teamList = this.list(queryWrapper);//得到查询出来的队伍列表 teamList
        if (CollectionUtils.isEmpty(teamList)){//如果 teamList 列表为空
            return new ArrayList<>();//返回空数组列表
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();

        //关联查询用户信息
        //5. 关联查询已加入队伍的用户信息
        //6. 关联查询已加入队伍的用户信息（可能会很耗费性能，建议大家用自己写 SQL 的方式实现）
        //SQL
        // 查询队伍和创建人的信息
        // select * from team t left join user u on t.userId = u.id
        // 查询队伍和已加入队伍成员的信息
        /**
         * select *
         *         from team t
         *                 left join user_team ut on t.id = ut.teamId
         *                 left join user u on ut.userId = u.id;
         */

        for (Team team : teamList){
            Long userId = team.getUserId();
            if (userId == null){
                continue;
            }
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);// team 赋值给 teamUserVO
            //脱敏用户信息
            User user = userService.getById(userId);
            if (user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);// user 赋值给 userVO
                teamUserVO.setCreateUser(userVO);//设置创建人信息
            }
            teamUserVOList.add(teamUserVO);//添加到表中
        }
        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User loginUser) {
        //1. 判断请求参数是否为空
        if (teamUpdateRequest == null){//判空
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 查询队伍是否存在
        Long id = teamUpdateRequest.getId();
        Team oldTeam = getTeamById(id);
        //3. 只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId()
                && !userService.isAdmin(loginUser)){// 队伍的创建者不是登录用户 并且 登录用户不是管理员
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //4. 如果用户传入的新值和老值一致，就不用 update 了（可自行实现，降低数据库使用次数）

        //5. 如果队伍状态改为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());//状态
        if (statusEnum.equals(TeamStatusEnum.SECRET)){//判断队伍状态是否为加密
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())){//如果密码为空
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密房间必须要有密码");
            }
        }
        //6. 更新成功
        Team updateTeam = new Team();
        //teamUpdateRequest 拷贝到 updateTeam
        BeanUtils.copyProperties(teamUpdateRequest,updateTeam);
        boolean result = this.updateById(updateTeam);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest,User loginUser) {
        if (teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //2. 队伍必须存在，只能加入未满、未过期的队伍
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);//判断队伍是否存在
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        //4. 禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (teamStatusEnum.equals(TeamStatusEnum.PRIVATE)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入私有队伍");
        }

        //5. 如果加入的队伍是加密的，必须密码匹配才可以
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
            }
        }
        //1. 用户最多加入 5 个队伍()
        long userId = loginUser.getId();//得到登录用户ID

        // 只有一个线程能获取锁
        RLock lock = redissonClient.getLock("yupao:join_team");//锁名称
        try{
            //抢到锁并执行
            while (true){
                if (lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                    System.out.println("getLock: "+ Thread.currentThread().getName());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();//条件构造器
                    userTeamQueryWrapper.eq("userId",userId);//相等条件
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);//得到加入队伍数量
                    if (hasJoinNum > 5){//如果加入队伍数量大于5
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多加入五个队伍");
                    }
                    //3. 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
                    userTeamQueryWrapper = new QueryWrapper<>();//复用条件构造器
                    userTeamQueryWrapper.eq("teamId",teamId);//相等条件
                    userTeamQueryWrapper.eq("userId",userId);//相等条件
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);//得到加入队伍数量
                    if (hasUserJoinTeam > 0){//如果加入队伍数量大于5
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已加入该队伍");
                    }

                    //队伍已加入人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()){//队伍加入人数大于队伍最大人数
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
                    }
                    //6. 新增队伍 - 用户关联信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean result = userTeamService.save(userTeam);
                    return result;
                }
            }
        } catch (InterruptedException e){
            log.error("doCacheRecommendUser error",e);//异常输出日志
            return false;
        }finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()){//判断是不是自己的锁
                System.out.println("unLock: "+Thread.currentThread().getName());
                lock.unlock();//解锁
            }
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();//从请求中得到队伍id
        Team team = getTeamById(teamId);
        //校验是否加入了队伍
        long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);

        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1){//队伍只剩一人 解散
            //删除队伍和所有加入队伍的关系
            this.removeById(teamId);
        }else {//还有其他人
            //判断是否是队长
            if (team.getUserId() == userId){//是队长
                //把队伍转移给最早加入的用户
                //1.查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();//复用条件构造器
                userTeamQueryWrapper.eq("teamId",teamId);//相等条件
                //按主键 id升序或时间查找两条数据
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);//得到第二加入队伍的人
                Long nextTeamLeaderId = nextUserTeam.getUserId();//得到继承者id
                Team updateTeam = new Team();//新建 修改队伍容器
                updateTeam.setId(teamId);//
                updateTeam.setUserId(nextTeamLeaderId);//设置队长
                boolean result = this.updateById(updateTeam);//修改
                if (!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍队长失败");
                }
            }
        }
        //移除关联(队长退出 用户退出)
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id,User loginUser) {
        //1. 校验请求参数(前面请求过了)
        //2. 校验队伍是否存在
        Team team = getTeamById(id);//获取队伍信息
        long teamId = team.getId();//获取队伍Id
        //3. 校验你是不是队伍的队长
        if (team.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //4. 移除所有加入队伍的关联信息
        //查询已加入队伍的所有用户和加入时间
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();//条件构造器
        userTeamQueryWrapper.eq("teamId", teamId);//相等条件
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍关联信息失败");
        }
        //5. 删除队伍
        return this.removeById(teamId);
    }


}




