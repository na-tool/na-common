package com.na.common.utils;

import java.util.List;

/**
 * 树形节点接口: 业务实体(如菜单、部门、地区)实现本接口后,
 * 即可直接用 {@link TreeUtil#toTree} 把平铺列表组装成树
 * <p>
 * <b>实体实现示例</b>:
 * <pre>{@code
 * @Data
 * public class Menu implements TreeNode<Long> {
 *     private Long id;
 *     private Long parentId;
 *     private Integer orderNum;        // 同级排序号, 允许为 null(排最后)
 *     private Integer level;           // 由 TreeUtil 组树时回填, 从 1 开始
 *     private List<Menu> children = new ArrayList<>();
 *
 *     @Override public Long getId()              { return id; }
 *     @Override public Long getParentId()        { return parentId; }
 *     @Override public Integer getOrderNum()     { return orderNum; }
 *     @Override public Integer getLevel()        { return level; }
 *     @Override public void setLevel(Integer level) { this.level = level; }
 *     @Override public List<? extends TreeNode<Long>> getChildren() { return children; }
 *     @Override public void setChildren(List<? extends TreeNode<Long>> children) {
 *         this.children = new ArrayList<>(children);
 *     }
 * }
 * }</pre>
 *
 * @param <ID> 节点 ID 类型, 如 Long / String
 */
public interface TreeNode<ID> {

    /** 获取节点 ID(要求全表唯一, 重复 ID 会导致组树异常) */
    ID getId();

    /** 获取父节点 ID; 顶级节点的 parentId 应等于组树时传入的 rootParentId */
    ID getParentId();

    /** 获取同级排序号(升序); 允许为 null, 为 null 时排在同级最后 */
    Integer getOrderNum();

    /** 设置子节点列表(TreeUtil 组树时调用) */
    void setChildren(List<? extends TreeNode<ID>> children);

    /** 获取子节点列表; 无子节点时由 TreeUtil 置为空列表, 保证非 null */
    List<? extends TreeNode<ID>> getChildren();

    /** 获取当前层级(根节点为 1, 子节点依次递增); 由 TreeUtil 组树时回填 */
    Integer getLevel();

    /** 设置当前层级(TreeUtil 组树时调用) */
    void setLevel(Integer level);
}
