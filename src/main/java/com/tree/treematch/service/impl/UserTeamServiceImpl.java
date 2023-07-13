package com.tree.treematch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tree.treematch.mapper.UserTeamMapper;
import com.tree.treematch.model.domain.UserTeam;
import com.tree.treematch.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author tree
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-05-16 23:18:32
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




