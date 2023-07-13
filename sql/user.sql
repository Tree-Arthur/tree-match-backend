-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userName     varchar(256)                       null comment '用户昵称',
    userAccount  varchar(256)                       null comment '账号',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    gender       tinyint                            null comment '性别',
    profile      varchar(512)                       null comment '个人简介',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment '用户状态 0-正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    userRole     int      default 0                 not null comment '用户权限 0-普通用户 1-管理用户',
    planetCode   varchar(512)                       null comment '编号',
    tags         varchar(1024)                      null comment '标签 json 列表',
    userPassword varchar(512)                       not null comment '用户密码'
)
    comment '用户';