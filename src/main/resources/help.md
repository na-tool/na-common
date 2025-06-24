### 公共模块

```text
本版本发布时间为 2021-05-01  适配jdk版本为 1.8
```

#### 1 配置
##### 1.1 添加依赖
```
<dependency>
    <groupId>com.na</groupId>
    <artifactId>na-common</artifactId>
    <version>1.0.0</version>
</dependency>
        
或者

<dependency>
    <groupId>com.na</groupId>
    <artifactId>na-common</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../lib/na-common-1.0.0.jar</systemPath>
</dependency>

相关依赖

        <dependency>
            <groupId>commons-beanutils</groupId>
            <artifactId>commons-beanutils</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId> <!-- 依赖 Jackson 用于 JSON 解析 -->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-data-25</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.lionsoul</groupId>
            <artifactId>ip2region</artifactId>
            <version>2.7.0</version>
        </dependency>
        
        <!-- 某些版本Tomcat需要以下依赖 -->
        <dependency>
            <groupId>org.jboss.xnio</groupId>
            <artifactId>xnio-api</artifactId>
            <version>3.8.7.Final</version>  <!-- 与 Undertow 2.2.x 兼容的版本 -->
        </dependency>
        <dependency>
            <groupId>org.jboss.xnio</groupId>
            <artifactId>xnio-nio</artifactId>
            <version>3.8.7.Final</version>  <!-- 与 Undertow 2.2.x 兼容的版本 -->
        </dependency>
        
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>3.0.0</version> <!-- 或更高版本 -->
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>3.0.0</version> <!-- 或更高版本 -->
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <scope>provided</scope>
        </dependency>
        <!-- 二维码-->
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
            <scope>provided</scope>
        </dependency>
```

##### 1.2 可能需要升级以下依赖
```
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>1.2.17</version>
        </dependency>
        <!-- mysql-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-extension</artifactId>
            <version>3.5.7</version>
        </dependency>

```

##### 1.3 配置
```yaml
na:
  swagger:
# 默认为 true，false则不启用
    enabled: true
    description: "NA平台"
# 单体是必填
    package: "com.na"
    title: "NA平台"
    termsOfServiceUrl: "http://www.naXXX.com"
    license: "NA平台"
    licenseUrl: "http://www.naXXX.com"
    version: "1.0.0"
    contact:
      name: na
      email: na_tool@163.com
      url: https://www.naXXX.com
# 开启跨域
  cors: true
# mybatis-plus insert update 时候自动更新时间
  sqlTime: true
# mybatis-plus 分页
  mybatis: true
# redis 常规操作并提供序列化方式
  redis: true
# 通过 Redisson 客户端操作 Redis , 提供对 Redis 更高级功能的支持，例如分布式锁、信号量、布隆过滤器等
  redissonClient: true
# 监听 Redis 的发布/订阅消息 , 这里用于监听 Redis 的特定事件（如键过期事件）
  redisListener: true
# 实现自定义的键过期事件处理逻辑
  redisMessage: true
# HTTP 请求工厂 , 支持简单的同步 HTTP 调用
  rest: true
# 配置 Jackson 的 ObjectMapper 实例, 用于序列化和反序列化 JSON 数据 , 自定义了数据格式转换防止精度丢失等
  jackson: true
# 错误页跳转
  errorPage: true
# 处理跨域  【注意】cors二选一，一般选择 cors
  corsFilter: true
```

##### 1.4 项目pom的特殊配置
```
    <build>
        <finalName>${project.name}</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.na.NAServiceApplication</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>2.5.5</version>
                <configuration>
                    <!-- 包含systemPath指定的依赖 -->
                    <includeSystemScope>true</includeSystemScope>
                </configuration>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/java</directory>
                <includes>
                    <include>**/*.xml</include>
                </includes>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>**/*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
```

##### 1.5 在使用bootstrap.yaml名称的配置文件时可能需要引入以下依赖
```
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>1.28</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-bootstrap</artifactId>
        <version>3.0.4</version>
        <scope>provided</scope>
    </dependency>
```

#### 2 使用
    启动类上使用 @EnableNA
    如果只引入 swagger 只使用 @EnableSwagger2WebMvc
##### 2.1 swagger配置
```
na:
  swagger:
# 默认为 true，false则不启用
    enabled: true
    description: "NA平台"
# 单体是必填
    package: "com.na"
    title: "NA平台"
    termsOfServiceUrl: "http://www.naXXX.com"
    license: "NA平台"
    licenseUrl: "http://www.naXXX.com"
    version: "1.0.0"
    contact:
      name: na
      email: na_tool@163.com
      url: https://www.naXXX.com
```
```java
@Configuration
@ConditionalOnProperty(name = "na.swagger.group", havingValue = "true")
public class MySwaggerConfig extends NaSwaggerConfig {

    @Autowired
    private Environment environment;

    @Bean("auth")
    public Docket authDocket()
    {
        return createDocket("auth", "com.na.controller.auth");
    }

    @Bean("common")
    public Docket commonDocket()
    {
        return createDocket("common", "com.na.controller.common");
    }
    

    private Docket createDocket(String groupName, String basePackage) {
        boolean swaggerEnabled = Boolean.parseBoolean(environment.getProperty("na.swagger.enabled", "true"));
        if (!swaggerEnabled) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET, responseMessages())
                .globalResponseMessage(RequestMethod.POST, responseMessages())
                .globalResponseMessage(RequestMethod.PUT, responseMessages())
                .globalResponseMessage(RequestMethod.DELETE, responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.newArrayList(apiKey()));
    }

}

```

##### 2.2 【重点】：如果使用网关，则下面的单体项目使用common模块需要配置如下,如果不关闭则会显示多次跨域，因为网关已经配置了允许跨域
```
使用哪个跨域 关闭哪个跨域
na:
  cors: false
```

##### 2.3 网关单独使用日志配置
```
在resources目录下添加 logback-spring.xml 文件
配置内容如下

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="log.path" source="logging.path" defaultValue="logs"/>
    <springProperty scope="context" name="spring.application.name" source="spring.application.name"/>
    <springProperty scope="context" name="spring.profiles.active" source="spring.profiles.active"/>
    <springProperty scope="context" name="common-pattern" source="logging.common-pattern"
                    defaultValue="%d{yyyy-MM-dd HH:mm:ss.SSS}:[%5p] [%t:%r] [%logger{50}.%M:%L] --> %msg%n"/>
    <springProperty scope="context" name="log.level.console" source="logging.level.console" defaultValue="INFO"/>
    <springProperty scope="context" name="log.level.controller" source="logging.level.controller" defaultValue="INFO"/>
    <springProperty scope="context" name="log.level.service" source="logging.level.service" defaultValue="INFO"/>
    <springProperty scope="context" name="log.level.dao" source="logging.level.sql" defaultValue="DEBUG"/>
    <springProperty scope="context" name="log.level.nacos" source="logging.level.nacos" defaultValue="WARN"/>

    <contextName>${spring.application.name}-logback</contextName>

    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <jmxConfigurator/>

    <!-- 控制台实时输出，采用高亮语法，用于开发环境 -->
    <appender name="CONSOLE_APPENDER" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${log.level.console}</level>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS}:[%5p] [%t:%r] [%cyan(%logger{50}).%M:%L] --> %highlight(%msg) %n
            </pattern>
        </encoder>
    </appender>
    <!-- 控制台异步实时输出 -->
    <appender name="ASYNC_CONSOLE_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <!-- 不丢失日志.默认的,如果队列的80%已满,则会丢弃TRACT、DEBUG、INFO级别的日志 -->
        <discardingThreshold>0</discardingThreshold>
        <!-- 更改默认的队列的深度,该值会影响性能.默认值为256 -->
        <queueSize>256</queueSize>
        <!-- 添加附加的appender,最多只能添加一个 -->
        <appender-ref ref="CONSOLE_APPENDER"/>
    </appender>

    <!-- 整个项目的所有日志， 包括第三方包 -->
    <appender name="ROOT_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/root.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 每天一归档 -->
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/root-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_ROOT_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="ROOT_APPENDER"/>
    </appender>

    <appender name="PROJECT_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/project.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 每天一归档 -->
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/project-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_PROJECT_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="PROJECT_APPENDER"/>
    </appender>

    <!-- 整个项目第三方包下的的日志 -->
    <appender name="THIRD_PARTY_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/third_party.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 每天一归档 -->
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/third_party-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_THIRD_PARTY_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="THIRD_PARTY_APPENDER"/>
    </appender>

    <!-- service包的日志 -->
    <appender name="SERVICE_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/service-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_SERVICE_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="SERVICE_APPENDER"/>
    </appender>

    <!-- controller 包的日志 -->
    <appender name="CONTROLLER_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/controller.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/controller-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_CONTROLLER_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="CONTROLLER_APPENDER"/>
    </appender>


    <!-- dao层日志，用于打印执行的sql  -->
    <appender name="DAO_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/dao.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/dao-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_DAO_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="DAO_APPENDER"/>
    </appender>

    <!-- 共用异常包的日志 -->
    <appender name="EXCEPTION_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${log.path}/${spring.application.name}/exception.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${log.path}/${spring.application.name}/%d{yyyy-MM}/exception-%d{yyyy-MM-dd}-%i.log.gz
            </fileNamePattern>
            <!-- 单个日志文件最多 100MB, 30天的日志周期，最大不能超过10GB -->
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${common-pattern}</pattern>
        </encoder>
    </appender>
    <appender name="ASYNC_EXCEPTION_APPENDER" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>256</queueSize>
        <appender-ref ref="EXCEPTION_APPENDER"/>
    </appender>

    <logger name="com.na.controller" additivity="true"
            level="${log.level.controller}">
        <appender-ref ref="CONTROLLER_APPENDER"/>
    </logger>
    <logger name="com.na.service" additivity="true"
            level="${log.level.service}">
        <appender-ref ref="SERVICE_APPENDER"/>
    </logger>

    <root level="${log.level.console}">
        <appender-ref ref="CONSOLE_APPENDER"/>
        <appender-ref ref="ROOT_APPENDER"/>
    </root>
</configuration>
```

##### 2.4 自定义返回结果 可能需要
```java
@ApiModel
@Data
public class R<T> extends NaResult<T> {
    public R() {
        super();
    }

    public R(Integer code, String msg) {
        super(code, msg);
    }

    private R(Integer code, String msg, T data) {
        super(code, msg, data);
    }

    public static <T> R<T> success(T data) {
        return new R<>(NaStatus.SUCCESS.getCode(), NaStatus.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> successNotMsg(T data) {
        return new R<>(NaStatus.DEFAULT.getCode(), null, data);
    }

    public static R error(StatusEnum status){
        return new R(status.getCode(), status.getMsg());
    }
}



public enum StatusEnum implements INaStatusProvider {
    DEFAULT(0, "default", "默认"),
    ERROR(-1, "error", "失败"),
    EXCEPTION(-2, "exception", "异常"),
    SUCCESS(1, "success", "成功"),
    TEST(999, "test", " 测试"),

    ;

    private final Integer code;
    private final String enMsg;
    private final String zhMsg;

    private StatusEnum(Integer code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getEnMsg() {
        return enMsg;
    }

    @Override
    public String getZhMsg() {
        return zhMsg;
    }

    @Override
    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }
}
```

##### 2.5 监听redis失效消息
```
@Component
public class RedisMessageListener implements RedisKeyExpirationService {
    @Override
    public void run(String message, byte[] pattern) throws Exception {
        System.out.println("RedisMessageListener:" + message);
    }
}
```

### 有可能需要的配置
```
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UndertowSessionConfig implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            deploymentInfo.setSessionPersistenceManager(null); // 禁用持久化会话（根据需求启用/禁用）
        });
    }
}
```

### 统一UTF-8 可能需要配置
```
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 配置默认消息转换器的编码为 UTF-8
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof AbstractHttpMessageConverter) {
                ((AbstractHttpMessageConverter<?>) converter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }
}
```

```text
【本版本发布时间为 2025-06-20  适配jdk版本为 1.8】
```
#### 3 使用说明
##### 3.1 其他项目使用本项目应在启动类上进行配置
```java
@EnableNA
@ComponentScan(basePackages = {"com.na", "com.ziji.baoming"}) // 扫描多个包路径
```

##### 3.2 base的使用
```text
1. controller继承 NaBaseController 即可对分页进行判断
    if(NaStatus.SUCCESS != checkPageParams(pagingVO.getCurrentPage(), pagingVO.getCurrentPage())){
        return R.error(StatusEnum.ERROR);
        }

2. 分页请求对象需要继承NaBasePagesParam 获得分页功能

3. 实体继承NaBaseEntity 获得字符串主键 需要自己输入功能

4. 获取分页相应数据NaBasePagesDomain

    service层的使用
    List<XXVO> list = new ArrayList<>();
    IPage<XXModel> modelIPage = service.queryPaging(params);
    return NaResult.successNotMsg(NaBasePagesDomain.pages(modelIPage, list));
    
    repo层的使用
    Page<XXEntity> page = new Page<>(params.getCurrentPage(), params.getPageSize());
    IPage<XXEntity> iPage =  baseMapper.queryPaging(page, params);
    if(iPage != null && CollectionUtils.isNotEmpty(iPage.getRecords()))
    {
       return NaBasePagesDomain.iPages(iPage, NaObjConversionUtil.copyPropertiesList(iPage.getRecords(), XXModel.class));
    }
    return NaBasePagesDomain.iPages(iPage,new ArrayList<>());
```

##### 3.3 NaCacheTemplate缓存的使用
```text
1. 需要进行配置，参考1.3 配置
    建议配置 na.redis , na.redissonClient

/**
 * 删除指定缓存键
 * @param key 缓存键
 */
public static void clear(String key)

/**
 * 删除所有以 prefix 开头的缓存键
 * @param prefix 键前缀
 */
public static void clearByPrefix(String prefix)

/**
 * 删除所有包含特定字符串的缓存键
 * @param pattern 键模式
 */
public static void clearKeysWithPattern(String pattern)

/**
 * 存储对象为 JSON 字符串到 Redis 缓存（无过期时间）
 * @param key 缓存键
 * @param obj 缓存对象
 * @param <T> 对象类型
 */
public static <T> void setCache(String key, T obj)

/**
 * 存储对象为 JSON 字符串到 Redis 缓存，并设置过期时间
 * @param key 缓存键
 * @param obj 缓存对象
 * @param time 过期时间
 * @param timeUnit 时间单位
 * @param <T> 对象类型
 */
public static <T> void setCache(String key, T obj, Long time, TimeUnit timeUnit)

/**
 * 获取缓存中的字符串值
 * @param key 缓存键
 * @return JSON字符串，找不到返回 null
 */
public static String getCache(String key)

/**
 * 获取指定类型的缓存对象
 * @param key 缓存键
 * @param tClass 目标类型 Class
 * @param <T> 泛型类型
 * @return 目标类型对象，找不到或解析失败返回 null
 */
public static <T> T getCache(String key, Class<T> tClass)

/**
 * 获取指定类型缓存，如果不存在则调用回调函数获取数据
 * @param key 缓存键
 * @param tClass 目标类型 Class
 * @param function 缓存未命中时调用的函数
 * @param <T> 泛型类型
 * @return 缓存数据或回调结果
 */
public static <T> T getCache(String key, Class<T> tClass, Supplier<T> function)

/**
 * 获取缓存中的列表数据，转换为指定元素类型的 List
 * @param key 缓存键
 * @param tClass 列表元素类型
 * @param <T> 元素类型
 * @return 列表对象，缓存无数据返回空列表
 */
public static <T> List<T> getCacheList(String key, Class<T> tClass)

/**
 * 获取缓存中的 Set 类型数据，转换为指定元素类型的 Set
 * @param key 缓存键
 * @param clazz 元素类型
 * @param <T> 元素类型
 * @return Set 对象，缓存无数据返回 null
 */
public static <T> Set<T> getCacheSet(String key, Class<T> clazz)

/**
 * 获取指定类型缓存（集合），未命中时调用函数，返回元素类型为 B 的 List
 * @param key 缓存键
 * @param tClass 缓存中数据的原始类型（如 List.class）
 * @param function 缓存未命中时调用函数
 * @param itemClass 列表中元素的目标类型
 * @param <T> 缓存原始类型
 * @param <B> 列表元素类型
 * @return 转换后的 List<B>
 */
public static <T,B> List<B> getCacheList(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass)

/**
 * 获取指定类型缓存（集合），未命中时调用函数，返回元素类型为 B 的 Set
 * @param key 缓存键
 * @param tClass 缓存中数据的原始类型
 * @param function 缓存未命中时调用函数
 * @param itemClass 集合元素类型
 * @param <T> 缓存原始类型
 * @param <B> 集合元素类型
 * @return 转换后的 Set<B>
 */
public static <T,B> Set<B> getCacheSet(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass)

/**
 * 获取指定类型缓存（集合），未命中时调用函数，返回元素类型为 B 的双端队列 Deque
 * @param key 缓存键
 * @param tClass 缓存原始类型（如 List.class）
 * @param function 缓存未命中时调用函数
 * @param itemClass 队列元素类型
 * @param <T> 缓存原始类型
 * @param <B> 队列元素类型
 * @return 转换后的 Deque<B>
 */
public static <T,B> Deque<B> getCacheDeque(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass)

/**
 * 获取缓存键剩余过期时间，单位毫秒
 * @param key 缓存键
 * @return 剩余过期时间（毫秒），-1 表示无过期时间，null 表示不存在
 */
public static Long getRemainingExpireTime(String key)

/**
 * 判断缓存键是否存在
 * @param key 缓存键
 * @return true 存在，false 不存在
 */
public static boolean exists(String key)

/**
 * Redis Hash 中写入单个键值对
 * @param key Redis 键
 * @param item Hash 字段名
 * @param value 字段值
 * @return 是否成功
 */
public static boolean hset(String key, String item, Object value)

/**
 * Redis Hash 中写入单个键值对，并设置键过期时间
 * @param key Redis 键
 * @param item Hash 字段名
 * @param value 字段值
 * @param time 过期时间（秒）
 * @return 是否成功
 */
public static boolean hset(String key, String item, Object value, long time)

/**
 * 设置 Redis 键的过期时间（秒）
 * @param key Redis 键
 * @param time 过期时间（秒）
 * @return 是否成功
 */
public static boolean expire(String key, long time)

/**
 * 获取 Redis Hash 中字段数量
 * @param key Redis 键
 * @return 字段数量，key 不存在返回 0，异常返回 null
 */
public static Long getHashSize(String key)

/**
 * 获取 Redis Hash 中指定字段的值
 * @param key Redis 键（非空）
 * @param item Hash 字段（非空）
 * @return 字段值，异常或无效参数返回 null
 */
public static Object hget(String key, String item)

/**
 * 尝试获取分布式锁
 * @param lockKey 锁键
 * @param timeout 锁有效时间
 * @param unit 时间单位
 * @return 是否成功获取锁
 */
public static boolean tryLock(String lockKey, long timeout, TimeUnit unit)

/**
 * 扫描符合 keyPattern 的 Redis Hash 数据，并转换为目标类型列表
 * @param keyPattern Redis 键匹配模式
 * @param scanCount 扫描数量建议
 * @param targetType 目标类型 Class
 * @param <T> 目标类型泛型
 * @return 转换后的列表
 */
public static <T> List<T> scanRedisHashData(String keyPattern, int scanCount, Class<T> targetType)

/**
 * 批量写入 Map<String, T> 到 Redis Hash，并设置统一过期时间
 * @param dataMap 键值对 Map，key 为 Redis 键，value 为对应对象
 * @param targetType 对象类型
 * @param expireTime 过期时间（秒），可为空
 * @param <T> 对象类型泛型
 */
public static <T> void batchSaveAsRedisHash(Map<String, T> dataMap, Class<T> targetType, Long expireTime)


```

##### 3.4 监听redis缓存的使用
```text
1. 需要进行配置 na.redisListener , na.redisMessage
直接实现 INaRedisKeyExpirationService

```

##### 3.4 静态常量的使用
```text
INaGlobalConst和NaConst
```

##### 3.5 其他配置参考 1.3
```text

```

##### 3.6 异常处理
```text
1. 可以关闭全局异常处理，默认开启
na:
    api:
        exception:
                handler: false

2. 可以抛的异常
    NaBusinessException，403的NaForbiddenException，401的NaUnauthorizedException
```

##### 3.7 返回结果处理，参考 2.4 自定义返回结果 可能需要
```text

```

##### 3.8 工具类的使用
```text
1. 获取IP  NaAddressUtil 或 NaIpParseUtil（依赖三方）
2. 公共的  NaCommonUtil
3. 时间的 NaDateTimeUtil
4. 文件读取的  NaFileReadUtil
5. 文件路径或图片（含网络）资源处理  NaFileUtil
6. ID的 NaIDUtil
7. token NaJwtUtil
8. 对象互相转换 NaObjConversionUtil
9. 二维码  NaQrCodeUtil
10. API远程调用 NaRestTemplateUtil
11.获取bean容器上下文  NaSpringContextUtil
12. 树状结构
    例如：
        Long 指定主键和parentId类型
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public class ResMenuTreeVO implements Serializable, NaTreeNode<Long> {
            private Long id;
            private String menuName;
            private Long parentId;
            private String parentName;
            private Integer orderNum;
            private String path;
            private String component;
            private String query;
            private Integer isFrame;
            private Integer isCache;
            private String menuType;
            private String visible;
            private String status;
            private String requestUri;
            private String perms;
            private String icon;
            private String sysMenuType;
            private String remark;
            private Integer isDel;
            private List<ResMenuTreeVO> children;
        
            @Override
            public void setChildren(List<? extends NaTreeNode<Long>> children) {
                this.children = (List<ResMenuTreeVO>) children;
            }
        }
        
        0L 为 parentId的类型和值，获取顶层数据用
        List<ResMenuTreeVO> menuTree = NaTreeUtil.toTree(list,0L);
        
13. excel导入
    例如:
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public class CityImport {
            @NaExcelImportColumn(index = 0, name = "序号")
            private Integer id;
        
            @NaExcelImportColumn(index = 1, name = "名称")
            private String name;
        
            @NaExcelImportColumn(index = 2, name = "编码")
            private String code;
        }
        
            public static void main(String[] args) {
        MultipartFile file = null ;
        NaExcelImportResultUtil<CityImport> result = NaExcelImportResultUtil.getList(CityImport.class, file);

        List<CityImport> data = result.getData();
        List<String> errors = result.getErrorMessages();

        // 可根据情况返回解析结果
        if (!errors.isEmpty()) {
            System.out.println("导入失败！");
        }

        // 成功导入的处理，比如入库
        data.forEach(System.out::println); // 这里只是打印
    }


14. excel导出
    例如:
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public class UserExport {
            @NaExcelExportColumn(headerName = "用户ID", order = 1)
            private Long id;
        
            @NaExcelExportColumn(headerName = "用户名", order = 2)
            private String name;
        
            @NaExcelExportColumn(headerName = "注册时间（Date）", order = 3, dateFormat = "yyyy-MM-dd HH:mm:ss")
            private Date registerTime;
        
            @NaExcelExportColumn(headerName = "最后登录（LocalDateTime）", order = 4, dateFormat = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime lastLogin;
            
        }
        
        List<UserExport> list = service.queryAll();
        NaExcelExportResultUtil.download(response,
                list, UserExport.class,0,"全部人员");
                
                NaExcelExportResultUtil.download(list, UserExport.class,0,"D:/导出数据.xlsx");

15. 日志的使用
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!--引入pd-tools-log模块中提供的日志的基础配置-->
    <include resource="com/na/log/config/na-defaults.xml"/>

    <springProfile name="test,docker,prod">
        <logger name="com.na.controller" additivity="true"
                level="${log.level.controller}">
            <appender-ref ref="ASYNC_CONTROLLER_APPENDER"/>
        </logger>
        <logger name="com.na.service" additivity="true"
                level="${log.level.service}">
            <appender-ref ref="ASYNC_SERVICE_APPENDER"/>
        </logger>
    </springProfile>

    <springProfile name="dev">
        <logger name="com.na.controller" additivity="true"
                level="${log.level.controller}">
            <appender-ref ref="CONTROLLER_APPENDER"/>
        </logger>
        <logger name="com.na.service" additivity="true"
                level="${log.level.service}">
            <appender-ref ref="SERVICE_APPENDER"/>
        </logger>
    </springProfile>
</configuration>

16. 国际化配置na.i18n
    na.staticWebSource默认即可，如果为true。防止静态资源会被拦截，导致访问不到静态资源
    
    同时需要实现INaLanguageService接口，实现语言切换
        String language = LocaleContextHolder.getLocale().getLanguage();
            if(language.equals(Locale.SIMPLIFIED_CHINESE.getLanguage())){
                NaLocalLangUtil.setLocale("zh_CN");
            }
    
    同时需要配置i18n.properties文件，首先在resources目录下，新建i18n文件夹，
    然后创建messages.properties，messages_en_US.properties，messages_zh_CN.properties等文件

同时配置
spring:
    messages:
        encoding: utf-8
        basename: i18n/messages
        cache-duration: 3600s  # 热加载，设置为 -1 表示缓存永不过期
```


# 【注意】启动类配置
```
如果你的包名不是以com.na开头的，需要配置
@ComponentScan(basePackages = {"com.na", "com.ziji.baoming"}) // 扫描多个包路径
```
