package com.na.common.annotation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.*;

/**
 * NA 框架一键启动注解。
 *
 * <p><b>从 3.0.0 起本注解已降级为可选</b>：na-common 自身的全部 Bean 改由自动装配注册
 * （见 {@code com.na.common.autoconfigure.NaCommonAutoConfiguration} 与
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}），
 * 宿主不再需要本注解，也不再要求主类位于 {@code com.na} 包下。
 *
 * <p><b>变更点：已移除 {@code @ComponentScan(basePackages = "com.na")}</b>。
 * 它原先是 na-common 全部 Bean 的唯一注册入口，但会扫描整个 {@code com.na} 命名空间，
 * 把宿主与其它 na-* 模块的组件一并扫入，边界不可控；且宿主主类不在 {@code com.na} 下时
 * 整个模块静默失效（不报错、不加载）。
 *
 * <p><b>升级检查清单（重要）</b>：
 * <ol>
 *   <li>如果你依赖本注解顺带扫描宿主的组件，请确认这些组件的包都在
 *       {@code @SpringBootApplication} 所在包的子树内。散落在该子树之外的宿主组件
 *       （例如主类在 {@code com.example}，组件在 {@code com.na.xxx}）不会被扫描到，
 *       且<b>不会报错</b>，只会在注入时才暴露。</li>
 *   <li>若有这类组件，请在启动类上显式补回扫描范围，例如：
 *       {@code @ComponentScan(basePackages = {"com.example", "com.na"})}。</li>
 *   <li>原先由本注解注册的 na-common Bean（corsFilter、redisTemplate、messageSource 等）
 *       现在由自动装配提供，无需任何配置；其中 {@code messageSource} 与
 *       {@code redisTemplate} 会接管 Spring Boot 的默认实现，但不会覆盖宿主自己声明的。</li>
 *   <li>部分 Bean 改了名字：{@code container → redisMessageListenerContainer}、
 *       {@code applicationUtil → springContextUtil}、{@code customizer → naJacksonCustomizer}。
 *       按名称注入或 {@code getBean("container")} 的地方需要同步修改；按类型注入不受影响。</li>
 * </ol>
 *
 * <p><b>组合的注解</b>（保留了与宿主边界相关的能力，库无法代为决定）：
 * <ul>
 *   <li>{@link ServletComponentScan} - 扫描宿主主类所在包的 @WebServlet/@WebFilter/@WebListener</li>
 *   <li>{@link EnableAspectJAutoProxy} - 启用 AOP AspectJ 自动代理</li>
 *   <li>{@link EnableAsync} - 启用 @Async 异步方法</li>
 *   <li>{@link MapperScan} - 扫描 com.na.*.*.mapper 下的 Mapper 接口</li>
 * </ul>
 *
 * <p><b>使用方式</b>：放在 {@code @SpringBootApplication} 之上（可选，不加也能正常工作）。
 * <pre>{@code
 * @EnableNA
 * @SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * @author na
 */
@Order(1)
@ServletComponentScan
@EnableAspectJAutoProxy
@EnableAsync
@MapperScan("com.na.*.*.mapper")
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableNA {
}
