package com.zhush.common.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @ClassName TreeUtils
 * @Description 构建树形结构数据
 * @Author zhush
 * @Date 2026/6/25 星期四 22:56
 *
 **/
public class TreeUtils {

    /**
     * 根据节点列表构建树形结构数据
     *
     * @param treeNodes 节点列表
     * @return 树形结构数据
     */
    public static <T extends TreeNode<T>> List<T> buildTrees(List<T> treeNodes) {
        List<T> result = new ArrayList<>();
        Map<Long, T> map = new HashMap<>();
        for (T treeNode : treeNodes) {
            map.put(treeNode.getId(), treeNode);
        }

        for (T node : map.values()) {
            T parent = map.get(node.getParentId());
            if (Objects.nonNull(parent)) {
                parent.getChildren().add(node);
            } else {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 根据节点列表和父节点ID构建树形结构数据
     *
     * @param nodes   节点列表
     * @param parentId 父节点ID
     * @return 树形结构数据
     */
    public static <T extends TreeNode<T>> List<T> buildTrees(List<T> nodes, Long parentId) {
        List<T> tree = new ArrayList<>();
        for (T node : nodes) {
            if (parentId.equals(node.getParentId())) {
                tree.add(node);
            }
        }
        return tree;
    }

    /**
     * 获取子节点
     */
    public static <T extends TreeNode<T>> T getChildren(List<T> treeNodes, T rootNode) {
        for (T treeNode : treeNodes) {
            if (rootNode.getId().equals(treeNode.getParentId())) {
                rootNode.getChildren().add(getChildren(treeNodes, treeNode));
            }
        }
        return rootNode;
    }

}
