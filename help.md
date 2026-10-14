### 公共模块

```text
版本 3.0.0  适配 JDK 17  基于 Spring Boot 3.5.x（Jakarta EE 命名空间）
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
  # ---------- 默认开启：不配置即生效，置 false 关闭 ----------
  cors:
    enable: true            # 全局跨域（经网关时建议关掉，由网关统一处理）
  errorPage:
    enable: true            # 错误页（前后端分离、错误页由前端处理时置 false）
  mybatis:
    enable: true            # MyBatis-Plus 拦截器（分页 + 乐观锁）
    db-type: MYSQL          # 方言：MYSQL / POSTGRE_SQL / ORACLE / DM / SQL_SERVER
    max-limit: 500          # 单页最大条数，防止超大页拖垮 DB
    overflow: false          # 页码溢出是否回到首页附近
  sqlTime:
    enable: true            # 自动填充 createTime / updateTime / isDel
  i18n:
    enable: true            # 国际化
  staticWebSource: false    # 是否放行静态资源
  swagger:
    enable: true            # OpenAPI 文档（注意是 enable，不是 enabled）
  rest:
    enable: true            # RestTemplate Bean
    connect-timeout: 5000   # 连接超时（毫秒）
    read-timeout: 30000     # 读取超时（毫秒）
  ffmpeg:
    enable: true            # ffmpeg 配置注入
    path: /opt/ffmpeg/bin/ffmpeg   # 不配则按操作系统选默认值
    timeout-minutes: 2
    hls-time: 10
  api:
    exception:
      handler: true         # 全局异常处理

  # ---------- 默认关闭：需显式置 true ----------
  jackson:
    enable: false           # Jackson 自定义序列化（日期格式 / Long→String / BigDecimal）
  csrf:
    enabled: false          # CSRF 过滤器
  xss:
    enabled: false          # XSS 过滤器
  feign:
    enable: false           # Feign 内部调用校验

  # ---------- 需同时配置 Redis（spring.data.redis.host 非空 且 开关为 true） ----------
  redis:
    enable: false           # RedisTemplate 与 CacheTemplateUtil
  redisson:
    enable: false           # RedissonClient（注意是 redisson，不是 redissonClient）
    timeout: 3000
    retry-attempts: 3
    retry-interval: 1500
  redisListener:
    enable: false           # RedisMessageListenerContainer（发布/订阅）
  redisMessage:
    enable: false           # key 过期事件监听（会自动带上监听容器）
  submit:
    enable: false           # 防重复提交切面
```

【重要】本表的键名与代码**逐字对应**，含义与默认值见 3.14 配置项总表。
注意 `na.swagger.enable`（不是 enabled），也**没有** `na.corsFilter`、`na.redissonClient`
这两个键——它们曾经出现在旧版文档里，代码从未读取。

【关于文档信息】OpenApi 文档的标题、描述、联系人、版本等目前在代码中固定，
不通过 `na.swagger.description` 之类的键读取；需要定制请自行声明 OpenAPI Bean。



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
@ConditionalOnProperty(name = "na.swagger.group", matchIfMissing = false)
public class MySwaggerConfig extends GlobalSwaggerConfiguration {
    @Autowired
    private AutoGlobalSwaggerConfig autoGlobalSwaggerConfig;


    @Bean("auth")
    public Docket authDocket()
    {
        if (!autoGlobalSwaggerConfig.getEnabled()) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("auth")
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.na.controller.auth"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }

    @Bean("platform")
    public Docket platformDocket()
    {
        if (!autoGlobalSwaggerConfig.getEnabled()) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("platform")
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.na.controller.platform"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }

    @Bean("merchant")
    public Docket merchantDocket()
    {
        if (!autoGlobalSwaggerConfig.getEnabled()) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("merchant")
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.na.controller.merchant"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }

    @Bean("front")
    public Docket frontDocket()
    {
        if (!autoGlobalSwaggerConfig.getEnabled()) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("front")
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.na.controller.front"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }

    @Bean("common")
    public Docket commonDocket()
    {
        if (!autoGlobalSwaggerConfig.getEnabled()) {
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("common")
                .apiInfo(groupApiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.na.controller.common"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }
}

```

##### 2.2 【重点】：如果使用网关，则下面的单体项目使用common模块需要配置如下,如果不关闭则会显示多次跨域，因为网关已经配置了允许跨域
```
使用哪个跨域 关闭哪个跨域：本模块只提供 CorsFilter 一种方式，
经网关时把本模块的跨域关掉，由网关统一处理，避免响应头重复。

na:
  cors:
    enable: false
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
public class R<T> extends Result<T> {
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
        return new R<>(Status.SUCCESS.getCode(), Status.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> successNotMsg(T data) {
        return new R<>(Status.DEFAULT.getCode(), null, data);
    }

    public static R error(StatusEnum status){
        return new R(status.getCode(), status.getMsg());
    }
}



public enum StatusEnum implements IStatusProvider {
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
【版本 3.0.0  适配 JDK 17  基于 Spring Boot 3.5.x】
```
#### 3 使用说明
##### 3.1 其他项目使用本项目应在启动类上进行配置
```java
@EnableNA
@ComponentScan(basePackages = {"com.na", "com.ziji.baoming"}) // 扫描多个包路径
```

##### 3.2 base的使用
```text
1. controller继承 BaseController 即可对分页进行判断
    if(Status.SUCCESS != checkPageParams(pagingVO.getCurrentPage(), pagingVO.getCurrentPage())){
        return R.error(StatusEnum.ERROR);
        }

2. 分页请求对象需要继承BasePagesParam 获得分页功能

3. 实体继承BaseEntity 获得字符串主键 需要自己输入功能

4. 获取分页相应数据BasePagesDomain

    service层的使用
    List<XXVO> list = new ArrayList<>();
    IPage<XXModel> modelIPage = service.queryPaging(params);
    return Result.successNotMsg(BasePagesDomain.pages(modelIPage, list));
    
    repo层的使用
    Page<XXEntity> page = new Page<>(params.getCurrentPage(), params.getPageSize());
    IPage<XXEntity> iPage =  baseMapper.queryPaging(page, params);
    if(iPage != null && CollectionUtils.isNotEmpty(iPage.getRecords()))
    {
       return BasePagesDomain.iPages(iPage, ObjConversionUtil.copyPropertiesList(iPage.getRecords(), XXModel.class));
    }
    return BasePagesDomain.iPages(iPage,new ArrayList<>());
```

##### 3.3 CacheTemplateUtil缓存的使用
```text
1. 需要进行配置，参考1.3 配置
    本类需要 na.redis.enable=true（且已配置 spring.data.redis.host）；
    另可叠加 na.redisson.enable=true 获得分布式锁等高级能力（注意键名是 redisson，不是 redissonClient）

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
直接实现 IRedisKeyExpirationService

```

##### 3.4.1 静态常量的使用
```text
IGlobalConst和GlobalConst
```

##### 3.5 其他配置参考 1.3
```text

```

##### 3.6 异常处理
```text
1. 全局异常处理默认开启，可以关闭
na:
  api:
    exception:
      handler: false

2. 可以主动抛的异常（都属于非受检异常）
    BusinessException          业务异常，携带 HTTP 状态码与消息，见下方第 4 点
    UnauthorizedException      401 未授权
    ForbiddenException         403 禁止访问

    示例：
        throw new BusinessException("订单状态不允许取消", HttpStatus.BAD_REQUEST);
        throw new BusinessException(Status.USER_NOT_EXIST, HttpStatus.NOT_FOUND);

3. 统一拦截的异常类型
    BusinessException / UnauthorizedException / ForbiddenException
    MethodArgumentNotValidException          @RequestBody 校验失败
    BindException                            @Valid 表单校验失败
    ConstraintViolationException             @RequestParam 校验失败
    MissingServletRequestParameterException  缺少必填参数
    IllegalStateException / NullPointerException
    RuntimeException（会先解包出内部的 BusinessException）
    Exception（兜底，读方法上的 @ApiException 注解决定返回哪个状态）

4. 响应契约（3.0.0 起）
    - 用 BusinessException(Status.xxx, httpStatus) 构造时，响应体会保留该枚举的业务码；
      用字符串构造时没有业务码，回退为通用失败码 -1。
    - HTTP 状态码取构造时传入的值；传 null 兜底为 200
      （本模块约定"业务异常用 HTTP 200 + 业务码区分"，需要 401/403 请显式传入）。
    - 异常处理链路自身出错时，只回固定文案"服务端异常，请稍后重试"，
      具体异常写服务端日志，不再把异常类名与内部消息回传给客户端。

5. 异步方法异常
    @Async 方法未捕获的异常由 asyncExceptionHandler Bean 记录到日志。
```

##### 3.7 返回结果处理，参考 2.4 自定义返回结果 可能需要
```text

```

##### 3.8 工具类的使用
```text
1. 获取IP  AddressUtil 或 IpParseUtil（依赖三方）
2. 公共的  CommonUtil
3. 时间的 DateTimeUtil
4. 加解密 crypto 包  AESUtil / Base64Util / DigestUtil / HmacUtil / MD5Util / SM2Util / SM3Util / SM4Util（详见 3.10）
5. 文件路径或图片（含网络）资源处理  FileUtil
6. ID的 IDUtil
7. token JwtUtil
8. 对象互相转换 ObjConversionUtil
9. 二维码  QrCodeUtil
10. API远程调用 RestTemplateUtil
11.获取bean容器上下文  SpringContextUtil
12. 树状结构
    例如：
        Long 指定主键和parentId类型
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public class ResMenuTreeVO implements Serializable, TreeNode<Long> {
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
            public void setChildren(List<? extends TreeNode<Long>> children) {
                this.children = (List<ResMenuTreeVO>) children;
            }
        }
        
        0L 为 parentId的类型和值，获取顶层数据用
        List<ResMenuTreeVO> menuTree = TreeUtil.toTree(list,0L);
        
13. excel导入
    例如:
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public class CityImport {
            @ExcelImportColumn(index = 0, name = "序号")
            private Integer id;
        
            @ExcelImportColumn(index = 1, name = "名称")
            private String name;
        
            @ExcelImportColumn(index = 2, name = "编码")
            private String code;
        }
        
            public static void main(String[] args) {
        MultipartFile file = null ;
        ExcelImportResultUtil<CityImport> result = ExcelImportResultUtil.getList(CityImport.class, file);

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
            @ExcelExportColumn(headerName = "用户ID", order = 1)
            private Long id;
        
            @ExcelExportColumn(headerName = "用户名", order = 2)
            private String name;
        
            @ExcelExportColumn(headerName = "注册时间（Date）", order = 3, dateFormat = "yyyy-MM-dd HH:mm:ss")
            private Date registerTime;
        
            @ExcelExportColumn(headerName = "最后登录（LocalDateTime）", order = 4, dateFormat = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime lastLogin;
            
        }
        
        List<UserExport> list = service.queryAll();
        ExcelExportResultUtil.download(response,
                list, UserExport.class,0,"全部人员");
                
                ExcelExportResultUtil.download(list, UserExport.class,0,"D:/导出数据.xlsx");

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

16. 国际化配置
na:
  i18n:
    enable: true          # 是否启用国际化（默认 true，不配也生效）
  staticWebSource: false  # 是否放行静态资源（默认 false）

【翻译文件位置】本模块固定从 classpath 下的这两处按顺序查找，不需要另外配置 spring.messages.basename：
    static/i18n/messages   （排在前，优先匹配）
    messages               （兼容 Spring Boot 默认位置）
请在 resources 目录下建 static/i18n/ 文件夹，放入：
    messages.properties           默认兜底，内容写中文
    messages_zh_CN.properties     简体中文
    messages_en_US.properties     英文
    messages_ja_JP.properties     日文（按需）
properties 统一按 UTF-8 解析，可直接写中文。

【语言解析优先级】（从高到低）
    1. 查询参数 ?lang=en-US（由拦截器负责，优先级最高）
    2. 请求头 Accept-Language 的第一段，如 "zh-CN,zh;q=0.9" 取 "zh-cn"
    3. 业务方实现的 ILanguageService#setLanguage()
    4. 默认中文 zh-CN

【可选扩展点】需要按用户/租户偏好决定语言时，实现该接口并注册为 Bean；不实现则优雅降级为默认中文，不会启动失败：
    @Component
    public class MyLanguageService implements ILanguageService {
        @Override
        public void setLanguage() {
            LocalLangUtil.setLocale("zh_CN");
        }
    }

【取值】LocalLangUtil.get(key) 取翻译，key 不存在时返回 key 本身；
        LocalLangUtil.get(key, defaultValue) 可取默认值。
        状态类文案走 IStatusProvider#getLanguageMsg()。

【注意】未配置的语言不会回落到操作系统语言（fallbackToSystemLocale=false），
        而是回落到 messages.properties 的内容，保证同一份包在不同部署机上行为一致。
        
17. csrf配置
na:
  csrf:
    enabled: false               # 是否启用 CSRF 过滤器（默认 false，需显式开启）
    # 是否允许 Referer 为空的请求（默认 true = 不拦截，浏览器地址栏直接访问时 Referer 为空）
    allowBlankReferer: true
    # 请求路径白名单（如：/health、/login），支持 /* 单段通配
    csrfWhitePaths:
      - /health
      - /login
      - /api/*
    # 域名白名单，支持以下 5 种写法
    csrfWhiteDomains:
      - www.example.com            # 纯域名，任意端口
      - www.example.com:8080       # 精确端口
      - www.example.com:8000-9000  # 端口范围
      - 192.168.1.*                # IP 段通配
      - 192.168.1.10-192.168.1.20  # IP 区间

【注意】
1. 校验依据是 Referer 与当前请求的 host:port 是否一致，不校验协议与路径。
2. 过滤器位于 DispatcherServlet 之前，全局异常处理器接不到它抛出的异常，
   因此拒绝时由过滤器自己写出统一格式的 Result JSON（HTTP 200 + 业务码）。
3. 经网关转发时，网关通常已处理跨域与来源校验，这里可以不开。

18. xss配置
na:
  # 对于json 首先确保 na.jackson.enable=true
  jackson:
    enable: true
  xss:
    enabled: true
    # 排除路径
    excludePaths:
      - /login
      - /health
```

##### 3.9 防重复提交

```text
两个注解共用同一个开关, 并且都依赖 Redis: 需要配置 spring.data.redis.host 且 na.submit.enable=true。

na:
  submit:
    enable: true
```

**1. @NoRepeatSubmitIP —— 按 IP + 请求路径计数（推荐）**

只依赖客户端 IP 与请求路径，**不需要任何用户体系实现**，未登录场景也能用。

```java
@NoRepeatSubmitIP(value = 5000, num = 3, showRemainingTime = true)
@PostMapping("/submit")
public Result submit() { ... }
// 同一 IP 在 5 秒内最多提交 3 次
```

**2. @NoRepeatSubmit —— 按 用户ID + 认证头 + 请求路径 计数**

适用于已登录场景，需要宿主提供当前用户标识：

```java
@Component
public class NoRepeatSubmitUserServiceImpl implements INoRepeatSubmitUserService {
    @Override
    public String userId() {
        return CurrentUserHolder.getId();   // 换成你自己的取用户方式
    }
}
```

【注意】没有实现 INoRepeatSubmitUserService 时，只会**跳过"按用户 ID"这个切面并打印一条 WARN，
不会导致启动失败**。所以只想用 IP 维度防重时，不必实现该接口。

**3. 注解属性**

| 属性 | 默认值 | 说明 |
|------|--------|------|
| value | 50 | 时间窗口，单位**毫秒** |
| num | 1 | 窗口内允许的提交次数，超出即拒绝 |
| msg | 重复请求，请稍后重试 | 拒绝时的提示文案 |
| showRemainingTime | false | 是否在提示后追加剩余时间 |
| dateStartMsg / dateEndMsg / dateFormat | 见注解 | 剩余时间的前后缀与格式 |
| requestParams | false | 是否把请求参数并入 key（不同参数分别计数，会显著增加 Redis key 数量） |

**4. 可用性约定（重要）**

防重是"尽力而为"的辅助能力。以下情况都会**放行请求并记录日志**，绝不因为防重组件自身的问题阻断业务：

- 没有 HTTP 请求上下文（在定时任务、MQ 消费里调用被标注的方法）
- Redis 不可用（计数失败）
- 取不到客户端 IP，或 INoRepeatSubmitUserService 返回空

反之，计数确实超限时会抛 BusinessException，由全局异常处理器转成统一响应。

**5. 计数语义**

同一个 key 在窗口内**首次**自增时才设置过期时间，之后不再延长 —— 即固定窗口，不是滑动窗口。
计数与设置 TTL 由一段 Lua 脚本原子完成，并发请求不会读到同一个计数值而同时放行。


##### 3.10 加解密工具（crypto 包）

```text
8 个纯静态工具类，无 Spring 依赖，可直接调用。国密算法（SM2/SM3/SM4）基于 BouncyCastle。

AESUtil        AES 加解密（GCM 模式，带认证标签）
Base64Util     Base64 编解码（标准 / URL 安全两种变体）
DigestUtil     SHA-1 / SHA-256 / SHA-512 摘要
HmacUtil       HMAC 签名（HmacSHA256 / HmacSHA1 / HmacMD5）
MD5Util        MD5 哈希（32 位与 16 位中段）
SM2Util        国密非对称加解密与签名（sm2p256v1 曲线）
SM3Util        国密密码哈希（基础 / 加盐 / HMAC-SM3）
SM4Util        国密对称加解密（128 位分组与密钥）

常用示例：
    String key = AESUtil.generateKey(256);
    String encrypted = AESUtil.encrypt("敏感数据", key);
    String plain = AESUtil.decrypt(encrypted, key);

    String hash = DigestUtil.sha256("hello");
    String sign = HmacUtil.hmacSha256("data", "secretKey");
    boolean valid = HmacUtil.verify("data", "secretKey", sign);

    String hashSm3 = SM3Util.hash("hello");
    String signSm3 = SM3Util.hmacSm3("data", "secretKey");

【注意】
1. AESUtil 密文格式为 Base64(IV + 密文 + Tag)，每次加密使用随机 IV，
   同一明文两次加密结果不同，不要用密文做幂等键。
2. HmacUtil 的同一密钥+消息签名结果恒定，可以直接用作幂等键；
   验签必须使用与签名相同的算法变体。
3. SM3Util.hmacSm3 按 RFC 2104 处理密钥（超过 64 字节先哈希压缩），
   与其它标准实现互通。
4. 密钥不要硬编码，从配置中心或环境变量注入。
5. 解密接口对长度不足的密文显式抛 IllegalArgumentException（密文格式非法），
   不会抛数组越界之类的意外异常。
```

##### 3.11 Feign 内部调用校验

```text
用于：本应只允许服务间调用的接口，防止外部直接访问。

1. 开启校验（默认关闭）
na:
  feign:
    enable: true

2. 服务端：在 Controller 方法上标注 @Feign
    @Feign
    @GetMapping("/internal/user/{id}")
    public Result<User> getInternalUser(@PathVariable Long id) { ... }
    被 NoRepeatSubmitAOP 之外的 FeignAop 拦截：请求头 from=Y 且 na-feign=Y 才放行，否则抛 401。

3. 调用端：无需额外代码。FeignInnerRequestInterceptor 会自动注入标识头
    - na-feign: Y          所有 Feign 请求都带
    - from: Y              仅标注了 @Token 的方法带（表示该接口无需 Token）

    @Token
    @GetMapping("/health")
    public String health() { return "OK"; }

【注意】
1. 只有 na.feign.enable=true 时服务端校验才生效；不开则 @Feign 标注的接口**没有任何保护**。
2. 宿主未引入 feign-core 时，这两个组件都不会注册，不会出现 NoClassDefFoundError。
3. @Feign 标注的是服务端接口，与 OpenFeign 客户端的 @FeignClient 是两回事，不要混淆。
```

##### 3.12 错误页

```text
把常见 HTTP 错误码转发到静态错误页，避免用户看到容器原生的 Whitelabel 页面。

na:
  errorPage:
    enable: true     # 默认 true

默认映射（页面文件需存在于 classpath:static 下）：
    401 → /401.html    403 → /403.html    404 → /404.html    500 → /500.html

本模块的 jar 已自带这 4 个页面，开箱可用；想换成自己的页面，在项目的
resources/static 下放同名文件即可覆盖。

【注意】只对内嵌容器（Tomcat/Jetty/Undertow）生效。若前后端分离、错误页由前端或
        网关处理，建议显式关闭：na.errorPage.enable=false
```

##### 3.13 Jackson 自定义序列化

```text
na:
  jackson:
    enable: true     # 默认 false，需显式开启

开启后统一以下序列化行为（JacksonModule）：
    日期时间   LocalDateTime → "yyyy-MM-dd HH:mm:ss"
              LocalDate     → "yyyy-MM-dd"
              LocalTime     → "HH:mm:ss"
    Long       → 字符串，防止前端 JS 精度丢失
    BigInteger → 字符串，防止精度丢失
    BigDecimal → 去掉末尾 0 的普通十进制字符串，避免科学计数法

【注意】
1. Long 变成字符串会改变 JSON 字段类型，前端与第三方对接需按字符串解析。
2. BigDecimal 输出的是 JSON 字符串而不是数字，前端接收类型与数字不同。
3. JSON 请求体的 XSS 清洗也依赖本开关：需要同时开启 na.jackson.enable 与 na.xss.enabled，
   才会注册 XssStringJsonDeserializer（见 3.8 第 18 项）。
```

##### 3.14 配置项总表

```text
按"默认是否生效"分三档，避免配错。

【默认开启】不配置即生效，显式置 false 可关闭
    na.cors.enable              true    全局跨域
    na.errorPage.enable         true    错误页
    na.mybatis.enable           true    MyBatis-Plus 拦截器（分页 + 乐观锁）
    na.sqlTime.enable           true    自动填充 createTime/updateTime/isDel
    na.i18n.enable              true    国际化
    na.swagger.enable           true    OpenAPI 文档
    na.rest.enable              true    RestTemplate Bean
    na.ffmpeg.enable            true    ffmpeg 配置注入
    na.api.exception.handler    true    全局异常处理

【默认关闭】需显式置 true
    na.jackson.enable           false   Jackson 自定义序列化
    na.csrf.enabled             false   CSRF 过滤器
    na.xss.enabled              false   XSS 过滤器
    na.feign.enable             false   Feign 内部调用校验

【需同时配置 Redis】条件为 spring.data.redis.host 非空 且 对应开关为 true
    na.redis.enable             false   RedisTemplate 与 CacheTemplateUtil
    na.redisson.enable          false   RedissonClient
    na.redisListener.enable     false   RedisMessageListenerContainer
    na.redisMessage.enable      false   key 过期事件监听（会自动带上监听容器）
    na.submit.enable            false   防重复提交切面

【带默认值的属性】
    na.mybatis.db-type=MYSQL        na.mybatis.max-limit=500        na.mybatis.overflow=false
    na.rest.connect-timeout=5000    na.rest.read-timeout=30000
    na.redisson.timeout=3000        na.redisson.retry-attempts=3    na.redisson.retry-interval=1500
    na.staticWebSource=false
    na.ffmpeg.path / na.ffmpeg.timeout-minutes=2 / na.ffmpeg.hls-time=10
    na.csrf.allowBlankReferer=true  na.csrf.csrfWhitePaths=[]  na.csrf.csrfWhiteDomains=[]
    na.xss.excludePaths=[]

【注意】@ConditionalOnProperty / @ConditionalOnExpression 里的键名是原样字符串匹配，
        不参与 Spring 的 relaxed binding。所以必须按下表原样书写：
        是 na.redisMessage.enable，不是 na.redis-message.enable；
        是 na.redisListener.enable，不是 na.redis-listener.enable。
        （@ConfigurationProperties 绑定的属性，如 na.csrf.*、na.ffmpeg.*，两种写法都行。）
```

##### 3.15 状态码表（对外协议）

```text
每个状态项是四元组: (code, 内置英文, 内置中文, i18n key)。
中文环境返回内置中文; 其他语言优先按 msgKey 从 static/i18n/messages*.properties 取翻译,
key 不存在时回退内置英文。完整清单如下, 前端按 code 分支时以此为准。
```

| code | 枚举 | 内置中文 | i18n key |
|---|---|---|---|
| 0 | DEFAULT | 默认 | default |
| -1 | ERROR | 失败 | error |
| -2 | EXCEPTION | 异常 | exception |
| 1 | SUCCESS | 成功 | success |
| 999 | TEST | 测试 | test |
| 10010 | DATE_NOT_EXIST | 数据不存在: {0} | data.not.exist |
| 401 | USER_NEED_AUTHORITIES | 未登录 | unauthorized |
| 403 | USER_NO_ACCESS | 无权访问 | forbidden |
| 500 | SERVER_ERROR | 服务器错误 | server.error |
| 401 | USER_AUTH_EXPIRED | 授权失效，请重新授权 | auth.expired |
| 401 | USER_AUTHORITIES_EXPIRED | 授权过期，请退出重新登陆! | auth.token.expired |
| 10000 | INTERNAL_SERVER_ERROR_ARGS | 服务端异常: {0} | server.error.args |
| 10001 | PARAM_PAGE_NO_NOT_VALID | 参数pageNo无效 | param.pageNo.invalid |
| 10002 | PARAM_PAGE_SIZE_NOT_VALID | 参数pageSize无效 | param.pageSize.invalid |
| 10003 | RETRY_AFTER_REFRESH | 刷新后重试 | retry.after.refresh |
| 10004 | PARAMS_NOT_VALID | 参数无效，请检查后后重试！ | param.invalid |
| 10011 | PARAMS_FORMAT_ERROR | 参数格式错误，请检查后重试！ | param.format.error |
| 10005 | OVER_UP_LIMIT | 超出上限，请修改后重新尝试！ | over.limit |
| 10006 | PARSING_FAILED | 解析失败，请修改后重新尝试！ | parsing.failed |
| 10007 | OPT_OVER_AUTH | 操作超出授权范围，请联系管理员！ | auth.limit.exceeded |
| 10008 | AUTHORIZATION_EXPIRED | 授权过期，请联系相关人员! | auth.expired.general |
| 10009 | FAIL_REQUEST_REPETITION | 请求太快了,请稍后重试！ | repeat.request |
| 10012 | SYS_DEF_DATA_BAN_OPT | 存在系统默认数据禁止操作 | system.default.operation.forbidden |
| 10013 | BAN_DELETE | 存在禁止删除数据，请检查！ | delete.ban |
| 10014 | BAN_OPT | 存在禁止操作数据，请检查！ | operation.ban |
| 20001 | LOGIN_FAILED | 登录失败，请检查后重试或联系管理员！ | login.failed |
| 20002 | LOGIN_FAILED_USER_NOT_AUTH | 登录失败，用户未授权请联系管理员！ | login.user.not.auth |
| 30000 | TENANT_NOT_EXIST | 租户不存在，请联系管理员 | tenant.not.exist |
| 30001 | TENANT_ALREADY_EXIST | 租户已存在，请修改后重新尝试！ | tenant.exists |
| 30002 | TENANT_NOT_ENABLE | 租户未开启，请与管理员联系！ | tenant.not.enabled |
| 30003 | TENANT_NOT_MATCH | 租户不匹配，请联系管理员！ | tenant.not.match |
| 30004 | TENANT_AUTH_ERROR | 租户认证错误，请联系管理员！ | tenant.auth.error |
| 30005 | TENANT_BIND_USER | 租户已经绑定用户，请先进行解绑！ | tenant.bind.user |
| 30006 | TENANT_UNBIND_USER | 请先进行解绑后重试！ | tenant.unbind.required |
| 40000 | USER_NOT_EXIST | 用户不存在，请联系管理员 | user.not.exist |
| 40001 | USER_ALREADY_EXIST | 用户已存在，请修改后重新尝试！ | user.exists |
| 40002 | USER_NOT_ENABLE | 用户未开启，请与管理员联系！ | user.not.enabled |
| 40003 | USER_ALREADY_BIND_TENANT | 用户被绑定租户，请解绑后重试！ | user.bind.tenant |
| 40004 | USER_ALREADY_BIND_ROLE | 用户被绑定角色，请解绑后重试！ | user.bind.role |
| 40005 | USER_AUTH_ERROR | 用户认证错误，请联系管理员！ | user.auth.error |
| 40006 | USER_PARSING_FAILED | 用户解析失败，请联系管理员！ | user.parsing.failed |
| 50000 | ROLE_ALREADY_EXIST | 角色已存在，请修改后重新尝试！ | role.exists |
| 50001 | ROLE_ALREADY_BIND_USER | 角色已被用户绑定，请修改后重新尝试！ | role.bind.user |
| 50002 | ROLE_ALREADY_BIND_MENU | 角色已被路由绑定，请修改后重新尝试！ | role.bind.menu |
| 50004 | ROLE_ALREADY_DEFAULT | 角色存在默认角色不能删除，请修改后重新尝试！ | role.default.cannot.delete |
| 50003 | ROLE_NOT_EXIST | 角色不存在，请修改后重新尝试！ | role.not.exist |
| 60000 | AUTH_RESOURCE_ALREADY_DEFAULT | 存在系统默认权限资源，禁止删除，请检查或联系管理员后重新尝试！ | auth.resource.default |
| 60001 | AUTH_RESOURCE_ALREADY_EXIST | 资源权限已存在，请修改后重新尝试！ | auth.resource.exists |
| 60002 | AUTH_RESOURCE_NOT_EXIST | 资源权限不存在，请修改后重新尝试！ | auth.resource.not.exist |
| 70000 | MENU_ALREADY_EXIST | 菜单路由已存在，请修改后重新尝试！ | menu.exists |
| 70001 | MENU_NOT_EXIST | 菜单路由不存在，请修改后重新尝试！ | menu.not.exist |
| 70002 | MENU_ALREADY_BIND_ROLE | 路由已被角色绑定，请修改后重新尝试！ | menu.bind.role |
| 70006 | MENU_ALREADY_DEFAULT | 存在系统默认菜单路由，禁止删除，请检查或联系管理员后重新尝试！ | menu.default.cannot.delete |
| 70003 | MENU_ALREADY_BIND_TENANT | 菜单已被租户绑定，请修改后重新尝试！ | menu.bind.tenant |
| 70004 | MENU_PARENT_NOT_EXIST | 菜单路由父级不存在，请修改后重新尝试！ | menu.parent.not.exist |
| 70005 | MENU_PARENT_CHOOSE_ERROR | 菜单路由父级选择错误，请修改后重新尝试！ | menu.parent.choose.error |
| 90000 | JOB_TASK_NOT_EXIST | 任务不存在，请联系管理员 | job.task.not.exist |
| 90001 | JOB_TASK_ALREADY_EXIST | 系统任务已存在，请修改后重新尝试！ | job.task.exists |
| 90002 | JOB_TASK_ALREADY_START | 系统任务已开启，请关闭后，重新尝试！ | job.task.started |
| 90003 | JOB_TASK_NOT_EXIST_AGAIN | 系统任务不存在，请修改后重新尝试！ | job.task.not.exist.again |
| 100000 | DEPT_ALREADY_EXIST | 部门已存在，请修改后重新尝试！ | dept.exists |
| 100001 | DEPT_NOT_EXIST | 部门不存在，请修改后重新尝试！ | dept.not.exist |

```text
【3.0.0 起的变更】历史上若干项因复制粘贴共用同一个 code, 且与所在分段不匹配, 已拆分到
各分段的空闲码位：

    DATE_NOT_EXIST              10000 -> 10010   (与 INTERNAL_SERVER_ERROR_ARGS 撞码)
    PARAMS_FORMAT_ERROR         10004 -> 10011   (与 PARAMS_NOT_VALID 撞码)
    SYS_DEF_DATA_BAN_OPT        10008 -> 10012   (与 AUTHORIZATION_EXPIRED 撞码)
    BAN_DELETE                  10008 -> 10013
    BAN_OPT                     10008 -> 10014
    ROLE_ALREADY_DEFAULT        50002 -> 50004   (与 ROLE_ALREADY_BIND_MENU 撞码)
    MENU_ALREADY_DEFAULT        70002 -> 70006   (与 MENU_ALREADY_BIND_ROLE 撞码)
    JOB_TASK_NOT_EXIST_AGAIN    90002 -> 90003   (与 JOB_TASK_ALREADY_START 撞码)

    另有两处仅改名（码不变）：
    UN_BIND_USER_TENANT  -> TENANT_UNBIND_USER   归入租户段, 码 10005 -> 30006
    JOB_TASK_MOT_EXIST   -> JOB_TASK_NOT_EXIST_AGAIN   (原拼写有误)
    MENU_ALREADY_ALREADY_DEFAULT -> MENU_ALREADY_DEFAULT (原拼写重复)

    前端若按旧码分支, 需同步更新。数据库或日志里已落库的旧码不会自动迁移。

【401 是有意共用的】USER_NEED_AUTHORITIES、USER_AUTH_EXPIRED、USER_AUTHORITIES_EXPIRED
三者都表示"需要重新登录"，前端按 code === 401 跳登录页对三者都成立。
需要区分具体原因请用各自的 msgKey（unauthorized / auth.expired / auth.token.expired）。
```

# 【注意】启动类配置
```
从 3.0.0 起，na-common 自身的能力已由自动装配接管：宿主**不需要加 @EnableNA，
也不要求包名以 com.na 开头**。

如果你的项目里还有自己的组件位于主类包之外，仍需自行声明扫描范围，例如：
@ComponentScan(basePackages = {"com.ziji.baoming", "com.example.other"})

（旧写法 @ComponentScan(basePackages = {"com.na", ...}) 中列举 com.na 已无必要：
na-common 的 Bean 不再依赖组件扫描，且该写法会把其它 na-* 模块的组件一并扫入，边界不可控。）
```

### 3.0.0 变更要点

```
1. 装配方式：新增自动装配（autoconfigure 包 + AutoConfiguration.imports），
   不再依赖 @ComponentScan("com.na")。@EnableNA 保留但已降级为可选。
2. Bean 改名：container → redisMessageListenerContainer
             applicationUtil → springContextUtil
             customizer → naJacksonCustomizer
   按类型注入不受影响；按名称注入或 getBean("container") 的地方需要同步修改。
3. messageSource 与 redisTemplate：本模块的实现会接管 Spring Boot 的默认实现
   （Spring Boot 自己提供的那个会被替换），但不会覆盖宿主自己声明的同类型 Bean。
4. 与 Spring Boot 默认实现的关系：WebMvcConfigurer 等"类型会被无关 Bean 占用"的场景
   改为按 Bean 名让位，避免整个配置类不注册。
```
