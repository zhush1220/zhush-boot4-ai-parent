package com.zhush.common.tree;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @ClassName TreeNode
 * @Description 树形节点
 * @Author zhush
 * @Date 2026/6/25 星期四 22:52
 *
 **/
@Data
public class TreeNode<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private List<T> children = new ArrayList<>();
}
