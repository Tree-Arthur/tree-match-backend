package com.tree.treematch.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户包装类(脱敏)
 */
@Data
public class UserVO implements Serializable {
    /**
     * id
     */
    private long id;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户头像
     */
    private String avatarUrl;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 个人简介
     */
    private String profile;
    /**
     * 电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 标签列表json
     */
    private String tags;
    /**
     * 用户状态 0-正常
     */
    private Integer userStatus;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 用户权限 0-普通用户 1-管理用户
     */
    private Integer userRole;
    /**
     * 编号
     */
    private String planetCode;
    private static final long serialVersionUID = 1L;
}
