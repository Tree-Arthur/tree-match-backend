package com.tree.treematch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author tree
 */
@Data
public class UserLoginRequest implements Serializable {
    /**
     * 序列化ID
     */
    public static final long serialVersionUID = 3191241716373120793L;
    /**
     * 用户账号
     */
    private String userAccount;//用户账号
    /**
     * 用户密码
     */
    private String userPassword;//用户密码
}
