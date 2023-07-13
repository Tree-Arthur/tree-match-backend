package com.tree.treematch.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求参数
 */
@Data
public class DeleteRequest implements Serializable {
    //生成序列化ID使对象在序列化时保持唯一
    private static final long serialVersionUID = -1277060116902753377L;
    /**
     * id
     */
    protected long id;
}
