package com.na.common.annotation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.*;

/**
 * NA 框架一键启动注解
 * <p>
 * 标注在启动类上, 组合了组件扫描、MyBatis Mapper 扫描、AOP、异步、Servlet 组件扫描等常用功能,
 * 并导入 {@link EnableConfig} 注册框架级配置
 * <p>
 * <b>使用方式:</b> 放在 {@code @SpringBootApplication} 之上, 避免加载顺序问题
 * <pre>{@code
 * @EnableNA
 * @SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 * <p>
 * <b>组合的注解:</b>
 * <ul>
 *   <li>{@link ServletComponentScan} - 扫描 @WebServlet/@WebFilter/@WebListener</li>
 *   <li>{@link EnableAspectJAutoProxy} - 启用 AOP AspectJ 自动代理</li>
 *   <li>{@link EnableAsync} - 启用 @Async 异步方法</li>
 *   <li>{@link MapperScan} - 扫描 com.na.*.*.mapper 下的 Mapper 接口</li>
 *   <li>{@link ComponentScan} - 扫描 com.na 包及子包下的组件</li>
 * </ul>
 *
 * @author na
 */
@Order(1)
@ServletComponentScan
@EnableAspectJAutoProxy
@EnableAsync
@MapperScan("com.na.*.*.mapper")
@ComponentScan(basePackages = {"com.na"})
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        EnableConfig.class
})
public @interface EnableNA {
}
