package com.tree.treematch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求体
 *
 * @author tree
 */
@Data
public class TeamJoinRequest implements Serializable {
    /**
     * 序列化ID
     */
    public static final long serialVersionUID = 3191241716373120793L;
    /**
     * id
     */
    private Long teamId;
    /**
     * 密码
     */
    private String password;
}
