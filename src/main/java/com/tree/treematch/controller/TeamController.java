package com.tree.treematch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tree.treematch.common.BaseResponse;
import com.tree.treematch.common.DeleteRequest;
import com.tree.treematch.common.ErrorCode;
import com.tree.treematch.common.ResultUtils;
import com.tree.treematch.exception.BusinessException;
import com.tree.treematch.model.domain.Team;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.model.domain.UserTeam;
import com.tree.treematch.model.dto.TeamQuery;
import com.tree.treematch.model.request.TeamAddRequest;
import com.tree.treematch.model.request.TeamJoinRequest;
import com.tree.treematch.model.request.TeamQuitRequest;
import com.tree.treematch.model.request.TeamUpdateRequest;
import com.tree.treematch.model.vo.TeamUserVO;
import com.tree.treematch.service.TeamService;
import com.tree.treematch.service.UserService;
import com.tree.treematch.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 队伍接口
 * @author tree
 */
@Slf4j
@RestController
@RequestMapping("/team")
//@CrossOrigin(origins = "http://43.138.192.49", allowCredentials = "true")
//@CrossOrigin(origins = {"http://localhost:5173"}, allowCredentials = "true")
public class TeamController {

    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     * @param teamAddRequest 队伍添加请求
     * @param request
     * @return 队伍id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if (teamAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);//获取当前登录用户
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);//teamAddRequest 赋值到 team
        long teamId = teamService.addTeam(team,loginUser);//创建队伍
        return ResultUtils.success(teamId);//返回队伍Id
    }

    /**
     * 更新队伍
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request){
        if (teamUpdateRequest == null){//判空
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);//获取登录用户
        boolean result = teamService.updateTeam(teamUpdateRequest,loginUser);//队伍修改
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 查询队伍
     * @param id 队伍id
     * @return
     */
    @GetMapping ("/get")
    public BaseResponse<Team> getTeamById( long id){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    /**
     * 查询队伍列表
     * @param teamQuery 队伍查询包装类
     * @param request
     * @return
     */
    @GetMapping ("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);//是否管理员
        //1 查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,isAdmin);//查询
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        //2 判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();//构造查询器 查询user_team表
        try{
            User loginUser = userService.getLoginUser(request);//获取登录信息
            userTeamQueryWrapper.eq("userId",loginUser.getId());
            userTeamQueryWrapper.in("teamId",teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);//根据构造查询器 查询user_team表
            //已加入的队伍id集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team ->{
                boolean hasjoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasjoin);//设置
            });
        }catch (Exception e){}
        //3 查询已加入队伍的用户信息(人数)
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();//构造查询器
        userTeamJoinQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);//根据构造查询器 查询user_team表
        //队伍 id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team ->{
            team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new ArrayList<>()).size());//设置
        });
        return ResultUtils.success(teamList);
    }

    /**
     * 分页查询队伍列表
     * @param teamQuery 队伍查询包装类
     * @return
     */
    //todo 查询分页
    @GetMapping ("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery){
        if (teamQuery == null){//判断是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //新建 Team 对象
        Team team = new Team();
        //控制把teamQuery对象属性复制到team对象里
        BeanUtils.copyProperties(teamQuery,team);
        //分页对象
        Page<Team> page = new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        //构造查询条件
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 队伍加入请求
     * @param teamJoinRequest
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if (teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if (teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        long id = deleteRequest.getId();
        if ( deleteRequest == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);//获取登录用户
        boolean result = teamService.deleteTeam(id,loginUser);//删除队伍(队伍id,登录用户)
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取我创建队伍
     * @param teamQuery 队伍查询包装类
     * @param request
     * @return
     */
    @GetMapping ("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);//获取登录用户
        teamQuery.setUserId(loginUser.getId());//设置登录用户id到队伍查询包装类
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,true);//查询
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     * @param teamQuery 队伍查询包装类
     * @param request
     * @return
     */
    @GetMapping ("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);//获取登录用户
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();//构造查询器
        queryWrapper.eq("userId",loginUser.getId());//userId = 登录用户id
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        //取出不重复的队伍id(去重) 使用stream流 进行列表分组相同teamId为key一组
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());//根据teamId为key得到队伍id列表
        teamQuery.setIdList(idList);//设置 id列表 到 队伍查询包装类
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,true);//查询(增闭原则)
        return ResultUtils.success(teamList);
    }
}
