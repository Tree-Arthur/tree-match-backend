package com.tree.treematch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author tree
 */
@Data
public class UserRegisterRequest implements Serializable {
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
    /**
     * 密码检测
     */
    private String checkPassword;
    /**
     * 编号
     */
    private String planetCode;//编号
}
