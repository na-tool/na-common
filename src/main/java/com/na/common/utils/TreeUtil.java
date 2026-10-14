package com.na.common.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 树形结构工具类, 配合 {@link TreeNode} 接口使用
 * <p>
 * <b>使用约定与注意事项</b>:
 * <ul>
 *   <li>节点 ID 必须唯一, 否则组树可能产生错误结构甚至死循环</li>
 *   <li>parentId 既不等于 rootParentId、也无法匹配任何节点 ID 的"孤儿节点"会被静默丢弃</li>
 *   <li>组树会回填 level(根为 1, 逐层递增)并把叶子节点的 children 置为空列表(非 null)</li>
 * </ul>
 * <p>
 * 示例:
 * <pre>{@code
 * // list 为平铺的菜单数据, 顶级节点的 parentId 均为 0
 * List<Menu> tree = TreeUtil.toTree(list, 0L);
 * // 树转回平铺列表(前序遍历, 父在子前), 常用于权限校验/批量校验
 * List<Menu> flat = TreeUtil.flatten(tree);
 * }</pre>
 */
public final class TreeUtil {

    /**
     * 递归深度上限。
     * <p>正常业务树远达不到该深度; 触顶说明数据存在自引用或环,
     * 此时抛出可定位的异常, 而不是让 JVM 抛 StackOverflowError。
     */
    private static final int MAX_DEPTH = 512;

    /** 纯静态工具类, 禁止实例化 */
    private TreeUtil() {
    }

    /**
     * 将平铺的节点列表组装为树
     * <p>
     * 算法: 先整体按 orderNum 排序(稳定), 再按 parentId 分组建立索引,
     * 之后逐节点从索引中 O(1) 取子节点挂载, 整体 O(n log n),
     * 相比原实现"每层递归重新过滤整个列表"的 O(n²) 大幅降低大数据量下的耗时
     *
     * @param list         平铺节点列表, 允许为 null / 空
     * @param rootParentId 顶级节点的父 ID 约定值(如 0L 或 null), parentId 与之相等的节点视为根
     * @param <T>          节点实体类型
     * @param <ID>         节点 ID 类型
     * @return 树的根节点列表(按 orderNum 升序); 输入为空时返回空列表
     */
    public static <T extends TreeNode<ID>, ID> List<T> toTree(List<T> list, ID rootParentId) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 全量排序一次(稳定排序): orderNum 为 null 的节点排最后, 修复原实现对 null 排序号直接 NPE 的问题
        List<T> sorted = list.stream()
                .sorted(Comparator.comparing(TreeNode::getOrderNum,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 按 parentId 分组(分组保持排序后的顺序), 后续挂载子节点均为 O(1) 查找
        Map<ID, List<T>> childrenByParent = new HashMap<>();
        for (T node : sorted) {
            childrenByParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
        }

        // 根节点: parentId 等于 rootParentId 的节点
        List<T> rootList = childrenByParent.getOrDefault(rootParentId, new ArrayList<>());
        for (T root : rootList) {
            root.setLevel(1);
            attachChildren(root, childrenByParent, 1);
        }
        return rootList;
    }

    /**
     * 递归挂载 node 的所有子孙节点并回填层级
     *
     * @param node             当前节点(层级已回填)
     * @param childrenByParent parentId -> 子节点列表 的分组索引
     * @param depth            当前深度(根为 1)
     * @throws IllegalStateException 深度超过 {@link #MAX_DEPTH}, 说明数据存在自引用或环
     */
    private static <T extends TreeNode<ID>, ID> void attachChildren(T node, Map<ID, List<T>> childrenByParent, int depth) {
        if (depth > MAX_DEPTH) {
            // 自引用(如节点 A 的 parentId 恰为自身且等于 rootParentId)会让递归永不收敛,
            // 原来是直接 StackOverflowError, 这里换成可定位的数据异常
            throw new IllegalStateException(
                    "组树层级超过上限 " + MAX_DEPTH + ", 节点 ID 可能存在自引用或环: " + node.getId());
        }
        List<T> children = childrenByParent.get(node.getId());
        if (children == null) {
            // 叶子节点: 置为空列表, 保证调用方拿到的 children 永远非 null
            node.setChildren(new ArrayList<>());
            return;
        }
        // 子节点层级 = 父节点层级 + 1
        for (T child : children) {
            child.setLevel(node.getLevel() + 1);
        }
        node.setChildren(children);
        for (T child : children) {
            attachChildren(child, childrenByParent, depth + 1);
        }
    }

    /**
     * 将树展平为平铺列表(前序深度优先遍历, 父节点排在子节点之前)
     * <p>
     * 注意: 返回的是原节点对象的引用列表, 不复制对象, 修改元素会直接影响原树
     *
     * @param tree 树的根节点列表, 允许为 null / 空
     * @param <T>  节点实体类型
     * @return 平铺节点列表; 输入为空时返回空列表
     */
    public static <T extends TreeNode<?>> List<T> flatten(List<T> tree) {
        List<T> result = new ArrayList<>();
        if (tree == null || tree.isEmpty()) {
            return result;
        }
        for (T node : tree) {
            flattenNode(node, result, 1);
        }
        return result;
    }

    /**
     * 递归展平单个节点及其子树
     *
     * @param node   当前节点
     * @param result 结果列表
     * @param depth  当前深度(根为 1)
     * @throws IllegalStateException 深度超过 {@link #MAX_DEPTH}, 说明 children 引用成环
     */
    @SuppressWarnings("unchecked")
    private static <T extends TreeNode<?>> void flattenNode(T node, List<T> result, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException(
                    "展平层级超过上限 " + MAX_DEPTH + ", children 引用可能成环: " + node.getId());
        }
        // 先加父节点, 保证前序顺序
        result.add(node);

        List<? extends TreeNode<?>> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (TreeNode<?> child : children) {
                // 实体实现中 children 与节点本身同类型, 此处泛型通配导致必须强转
                flattenNode((T) child, result, depth + 1);
            }
        }
    }
}
