package com.tree.treematch.service;

import com.tree.treematch.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tree.treematch.model.domain.User;
import com.tree.treematch.model.dto.TeamQuery;
import com.tree.treematch.model.request.TeamJoinRequest;
import com.tree.treematch.model.request.TeamQuitRequest;
import com.tree.treematch.model.request.TeamUpdateRequest;
import com.tree.treematch.model.vo.TeamUserVO;

import java.util.List;

/**
* @author tree
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-05-16 23:16:08
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team 队伍
     * @param loginUser 登录用户
     * @return
     */
    long addTeam(Team team, User loginUser);
    /**
     * 查询队伍列表
     * @param teamQuery 队伍查询
     * @param isAdmin 是否为管理员
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery , boolean isAdmin);
    /**
     * 更新队伍
     * @param teamUpdateRequest
     * @param loginUser 登录的用户
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User loginUser);
    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest,User loginUser);
    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除(解散)队伍
     * @param id
     * @return
     */
    boolean deleteTeam(long id,User loginUser);
}
