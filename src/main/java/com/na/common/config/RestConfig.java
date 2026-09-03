package com.na.common.config;

import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.na.common.utils.RestTemplateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * RestTemplate 配置类: 向 Spring 容器注册一个预配置的 {@link RestTemplate} Bean
 * <p>
 * 该 Bean 与工具类 {@code RestTemplateUtil} 持有的静态实例配置保持一致(UTF-8 编码、相同默认超时),
 * 便于业务方在需要依赖注入(如 @Autowired RestTemplate)或使用 {@code @LoadBalanced} 服务发现调用的场景统一复用。
 * <p>
 * 生效条件: 配置项 {@code na.rest.enable} 未显式置为 false(默认开启)
 * <p>
 * 配置示例(application.yml):
 * <pre>{@code
 * na:
 *   rest:
 *     enable: true              # 是否注册 RestTemplate Bean, 默认 true
 *     connect-timeout: 5000     # 连接超时(毫秒), 默认 5000
 *     read-timeout: 30000       # 读取超时(毫秒), 默认 30000
 * }</pre>
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 方式一: 依赖注入(支持服务名负载均衡, 如 http://user-service/api/users)
 * @Autowired
 * private RestTemplate restTemplate;
 *
 * // 方式二: 直接获取 Bean
 * RestTemplate rt = springContextUtil.getBean(RestTemplate.class);
 * }</pre>
 * <p>
 * 注意事项:
 * <ul>
 *     <li>原实现 {@code getMessageConverters().clear()} 会清空默认转换器, 导致 multipart/form-data 上传、
 *     二进制流、text/* 等响应失效; 本实现改为"在首位插入 FastJson 转换器", 既让 JSON 优先走 FastJson,
 *     又保留 Spring 默认转换器作为其他 Content-Type 的兜底</li>
 *     <li>{@code @LoadBalanced} 启用服务发现负载均衡, 调用方可用服务名替代 IP:端口;
 *     若项目不使用 Spring Cloud 服务发现, 该注解不影响普通 URL 调用</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = {"na.rest.enable"},
        havingValue = "true",
        matchIfMissing = true
)
public class RestConfig {

    /**
     * 连接超时(毫秒), 默认 5000(与 RestTemplateUtil 默认值对齐)
     * <p>可通过配置 {@code na.rest.connect-timeout} 覆盖
     */
    @Value("${na.rest.connect-timeout:5000}")
    private int connectTimeout;

    /**
     * 读取超时(毫秒), 默认 30000(与 RestTemplateUtil 默认值对齐)
     * <p>可通过配置 {@code na.rest.read-timeout} 覆盖
     */
    @Value("${na.rest.read-timeout:30000}")
    private int readTimeout;

    /**
     * 注册名为 restTemplate 的 RestTemplate Bean
     * <p>
     * 配置内容:
     * <ol>
     *     <li>连接/读取超时: 默认 5s / 30s, 可通过配置项覆盖</li>
     *     <li>消息转换器: 在首位插入 UTF-8 编码的 FastJsonHttpMessageConverter, 处理 application/json;
     *     同时将默认 StringHttpMessageConverter 替换为 UTF-8 版本, 避免 text/* 响应中文乱码</li>
     * </ol>
     * <p>
     * 示例: 由 Spring 容器自动装配, 业务方通过 @Autowired 注入使用
     *
     * @return 预配置的 RestTemplate 实例(启用 @LoadBalanced 服务发现)
     */
    @Bean
    @LoadBalanced  // 启用 Spring Cloud 服务发现负载均衡, 支持按服务名调用
    public RestTemplate restTemplate() {
        // 1. 创建请求工厂并设置超时, 防止对端无响应时调用线程无限挂起
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // 2. 将默认的 StringHttpMessageConverter(ISO-8859-1) 替换为 UTF-8 版本, 防止 text/* 响应中文乱码
        Iterator<HttpMessageConverter<?>> it = restTemplate.getMessageConverters().iterator();
        while (it.hasNext()) {
            if (it.next() instanceof StringHttpMessageConverter) {
                it.remove();
            }
        }
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        // 3. 在转换器列表首位插入 FastJson(UTF-8), 让 application/json 优先走 FastJson 序列化
        FastJsonHttpMessageConverter fastJsonConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastJsonConverter.setFastJsonConfig(fastJsonConfig);
        // 插入首位: RestTemplate 按列表顺序匹配 Content-Type, 首位优先级最高
        restTemplate.getMessageConverters().add(0, fastJsonConverter);

        // 4. 同步覆盖 RestTemplateUtil 静态实例的超时, 使静态调用与 Bean 超时配置一致
        RestTemplateUtil.init(connectTimeout, readTimeout);

        return restTemplate;
    }
}
