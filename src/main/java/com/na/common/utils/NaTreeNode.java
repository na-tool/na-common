package com.na.common.utils;

import java.util.List;

public interface NaTreeNode<ID> {
    ID getId();                // 获取节点ID
    ID getParentId();          // 获取父节点ID
    Integer getOrderNum();       // 获取排序号
    void setChildren(List<? extends NaTreeNode<ID>> children);  // 设置子节点列表
}

