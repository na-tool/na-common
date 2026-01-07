package com.na.common.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NaTreeUtil {
    public static <T extends NaTreeNode<ID>, ID> List<T> toTree(List<T> list, ID rootParentId) {

        List<T> rootList = list.stream()
                .filter(node -> Objects.equals(rootParentId, node.getParentId()))
                .sorted(Comparator.comparingInt(NaTreeNode::getOrderNum))
                .collect(Collectors.toList());

        // ⭐ 根节点层级 = 1
        rootList.forEach(root -> root.setLevel(1));

        List<T> subList = list.stream()
                .filter(node -> !Objects.equals(rootParentId, node.getParentId()))
                .sorted(Comparator.comparingInt(NaTreeNode::getOrderNum))
                .collect(Collectors.toList());

        rootList.forEach(root -> findChild(root, subList));
        return rootList;
    }


    private static <T extends NaTreeNode<ID>, ID> void findChild(T root, List<T> subList) {

        List<T> childrenList = subList.stream()
                .filter(node -> Objects.equals(node.getParentId(), root.getId()))
                .sorted(Comparator.comparingInt(NaTreeNode::getOrderNum))
                .collect(Collectors.toList());

        if (!childrenList.isEmpty()) {
            for (T child : childrenList) {
                child.setLevel(root.getLevel() + 1); // ⭐ 关键
            }
            root.setChildren(childrenList);
            childrenList.forEach(child -> findChild(child, subList));
        } else {
            root.setChildren(new ArrayList<>());
        }
    }

}
