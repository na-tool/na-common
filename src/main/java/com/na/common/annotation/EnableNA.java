package com.na.common.annotation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE_USE;

@Order(1)  // 设置高优先级
/**
 * {@link EnableNA} 要放在@SpringBootApplication之上 避免出现一些奇怪的加载顺序问题
 */
/**
 * @ServletComponentScan: 扫描使用 WebServlet、WebFilter、WebListener 注解的类，并将它们注册到 Spring 容器中。
 * 使用场景：在 Spring Boot 应用中自动注册 Servlet、Filter 和 Listener。
 */
@ServletComponentScan

/**
 * @EnableAspectJAutoProxy: 启用 Spring AOP 的 AspectJ 自动代理支持。
 * 使用场景：在 Spring 应用中使用 AOP（面向切面编程）功能，如定义切面（Aspect）和切点（Pointcut）以实现横切关注点（如日志记录、事务管理等）。
 */
@EnableAspectJAutoProxy

/**
 * @EnableAsync: 启用 Spring 异步方法执行的支持。
 * 使用场景：在 Spring 应用中允许使用 @Async 注解的方法异步执行，从而实现异步调用。
 */
@EnableAsync

/**
 * @MapperScan: 扫描指定包路径下的 MyBatis Mapper 接口，并自动注册它们。
 * 使用场景：用于 MyBatis 项目中，自动扫描和注册 Mapper 接口。
 * 参数："com.na.*.*.mapper" 会扫描 com.na 包及其子包下的所有 mapper 包中的 Mapper 接口。
 */
@MapperScan("com.na.*.*.mapper")

/**
 * @ComponentScan: 指定要扫描的包，以自动发现和注册 Spring 组件（如 @Component、@Service、@Repository、@Controller 注解的类）。
 * 使用场景：用于指定 Spring 应用的基础包路径，以自动扫描并注册组件。
 * 参数：basePackages = {"com.na"} 会扫描 com.na包及其子包中的组件。
 */
@ComponentScan(basePackages = {"com.na"})

/**
 * @EnableSwagger2WebMvc: 启用 Swagger 2 的支持，用于生成 API 文档。
 * 使用场景：用于 Spring Boot 应用中，集成 Swagger 生成和展示 RESTful API 文档。
 */
@EnableSwagger2WebMvc

/**
 * @Inherited: 允许注解被子类继承。
 * 使用场景：当一个注解被标记为 @Inherited 时，如果该注解应用于一个类，那么它的子类将自动继承该注解。
 */
@Inherited

/**
 * @Target: 指定注解的应用目标。
 * 使用场景：在这个例子中，注解可以应用于类型使用（Type Use）。
 * 参数：{ TYPE_USE } 表示注解可以应用于类型使用。
 */
@Target({ TYPE_USE })

/**
 * @Retention: 指定注解的保留策略。
 * 使用场景：在这个例子中，注解将在运行时保留，可以通过反射机制读取。
 * 参数：RetentionPolicy.RUNTIME 表示注解将保留到运行时。
 */
@Retention(RetentionPolicy.RUNTIME)

/**
 * @Documented: 表明使用该注解的元素应被 javadoc 或类似工具记录。
 * 使用场景：通常用于生成文档，使注解信息包含在 javadoc 中。
 */
@Documented
@Import({
        EnableNAConfiguration.class // 导入自定义配置类
})
public @interface EnableNA {
}

