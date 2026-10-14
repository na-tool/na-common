package com.na.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 配置类: 注册拦截器链(分页 + 乐观锁)并开启声明式事务
 * <p>
 * 注册的拦截器:
 * <ul>
 *     <li>{@link PaginationInnerInterceptor}: 分页插件, 按数据库方言生成 LIMIT 语句;
 *     支持 maxLimit 限制单页最大条数, 防止恶意大页查询拖垮数据库</li>
 *     <li>{@link OptimisticLockerInnerInterceptor}: 乐观锁插件, 配合实体 @Version 字段实现并发更新 CAS</li>
 * </ul>
 * <p>
 * 生效条件: 配置项 {@code na.mybatis.enable} 未显式置为 false(默认开启)
 * <p>
 * 配置示例(application.yml):
 * <pre>{@code
 * na:
 *   mybatis:
 *     enable: true                # 是否注册拦截器, 默认 true
 *     db-type: MYSQL              # 数据库方言, 默认 MYSQL; PG 用 POSTGRE_SQL, Oracle 用 ORACLE, 达梦用 DM
 *     max-limit: 500              # 单页最大条数, 默认 500; 防止 select * limit 9999999 拖垮 DB
 *     overflow: false             # 页码溢出是否自动回到首页附近, 默认 false(溢出返回空)
 * }</pre>
 */
@ConditionalOnProperty(
        name = {"na.mybatis.enable"},
        havingValue = "true",
        matchIfMissing = true
)
public class MybatisPlusConfig {

    /**
     * 数据库方言, 默认 MYSQL
     * <p>对应 {@link DbType} 枚举名, 如 MYSQL / POSTGRE_SQL / ORACLE / DM / SQL_SERVER
     */
    @Value("${na.mybatis.db-type:MYSQL}")
    private String dbType;

    /**
     * 单页最大条数, 默认 500
     * <p>防止调用方传超大 size(如 9999999)导致 DB 一次返回大量数据拖垮内存与连接池
     */
    @Value("${na.mybatis.max-limit:500}")
    private Long maxLimit;

    /**
     * 页码溢出处理, 默认 false
     * <p>false: 超过总页数返回空; true: 自动回到首页附近(防止前端翻页越界返回空体验差)
     */
    @Value("${na.mybatis.overflow:false}")
    private boolean overflow;

    /**
     * 注册 MyBatis-Plus 拦截器链
     * <p>
     * 添加顺序: 分页在前, 乐观锁在后(遵循官方推荐顺序, 分页改写 SQL 的 limit 需先于乐观锁)
     * <p>
     * 示例: 由 Spring 容器自动装配, 业务方在 Mapper 继承 BaseMapper 即可使用
     * <pre>{@code
     * // 分页查询
     * Page<User> page = userMapper.selectPage(new Page<>(1, 10), queryWrapper);
     * // 乐观锁更新: 实体需有 @Version 字段
     * userMapper.updateById(user);  // version 自动 +1, 并带 version 条件
     * }</pre>
     *
     * @return 配置好分页与乐观锁插件的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页插件: 按配置的方言生成 LIMIT, 限制单页最大条数, 防止恶意大页
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(resolveDbType());
        if (maxLimit != null && maxLimit > 0) {
            paginationInterceptor.setMaxLimit(maxLimit);
        }
        paginationInterceptor.setOverflow(overflow);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 2. 乐观锁插件: 配合实体 @Version 字段, 更新时自动带 version 条件并 +1
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /**
     * 将配置的字符串方言名解析为 {@link DbType} 枚举
     * <p>非法值兜底返回 {@link DbType#MYSQL}, 避免启动失败阻塞业务
     *
     * @return 解析后的数据库方言枚举
     */
    private DbType resolveDbType() {
        try {
            return DbType.valueOf(dbType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 非法方言名兜底 MYSQL, 避免配置写错导致应用无法启动
            return DbType.MYSQL;
        }
    }
}
