package com.tree.treematch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户退出队伍请求体
 *
 * @author tree
 */
@Data
public class TeamQuitRequest implements Serializable {
    /**
     * 序列化ID
     */
    public static final long serialVersionUID = 3191241716373120793L;
    /**
     * id
     */
    private Long teamId;
}
