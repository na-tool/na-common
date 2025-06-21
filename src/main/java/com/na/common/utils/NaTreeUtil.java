package com.na.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NaTreeUtil {
    public static <T extends NaTreeNode<ID>, ID> List<T> toTree(List<T> list, ID rootParentId) {
        List<T> rootList = list.stream()
                .filter(node -> Objects.equals(rootParentId, node.getParentId()))
                .sorted((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()))
                .collect(Collectors.toList());

        List<T> subList = list.stream()
                .filter(node -> !Objects.equals(rootParentId, node.getParentId()))
                .sorted((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()))
                .collect(Collectors.toList());

        rootList.forEach(root -> findChild(root, subList));
        return rootList;
    }


    private static <T extends NaTreeNode> void findChild(T root, List<T> subList) {
        // 通过根节点去id和子节点的parentId是否相等，如果相等的话，代表是当前根的子集
        List<T> childrenList = subList.stream().filter(org -> org.getParentId().equals(root.getId()))
                .sorted((a, b) -> a.getOrderNum() - b.getOrderNum()).collect(Collectors.toList());

        // 如果当前有子集，初始化并放回去
        if (!childrenList.isEmpty()) {
            root.setChildren(childrenList);
            // 再次递归构建
            childrenList.forEach(org -> findChild(org, subList));
        } else {
            root.setChildren(new ArrayList<>());
        }
    }
}
