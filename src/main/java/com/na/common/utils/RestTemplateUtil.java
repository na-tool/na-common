package com.na.common.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 远程请求处理工具类(基于 RestTemplate)
 * <p>
 * 内部持有线程安全的共享 {@link RestTemplate} 实例(含 UTF-8 消息转换器与默认超时),
 * 避免每次请求重复创建实例与转换器列表。
 * <p>
 * 注意: RestTemplate 对 4xx/5xx 响应默认抛出 HttpClientErrorException / HttpServerErrorException,
 * 调用方需自行捕获; 所有方法均为静态调用
 */
public final class RestTemplateUtil {

    /** 默认连接超时(毫秒) */
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;

    /** 默认读取超时(毫秒) */
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;

    /**
     * 共享实例: RestTemplate 无状态、线程安全; 预置 UTF-8 转换器
     * <p>
     * 声明为 volatile: 保证 {@link #init(int, int)} 重建实例后, 其他线程立即可见新实例
     */
    private static volatile RestTemplate REST_TEMPLATE;

    static {
        // 类加载时用默认超时初始化, 保证不依赖 Spring 容器也能直接使用
        REST_TEMPLATE = createRestTemplate(DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /**
     * 按指定超时创建预配置的 RestTemplate(保留 Spring 默认转换器 + 追加 UTF-8 字符串转换器)
     * <p>
     * 抽取为私有方法, 供 static 块默认初始化与 {@link #init(int, int)} 覆盖配置复用
     *
     * @param connectTimeoutMillis 连接超时(毫秒)
     * @param readTimeoutMillis    读取超时(毫秒)
     * @return 预配置的 RestTemplate 实例
     */
    private static RestTemplate createRestTemplate(int connectTimeoutMillis, int readTimeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 超时: 防止对端无响应时调用线程无限挂起
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 追加 UTF-8 字符串转换器, 避免响应为 text/* 时中文乱码
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    /** 纯静态工具类，禁止实例化 */
    private RestTemplateUtil() {
    }

    /**
     * 获取共享的 RestTemplate 实例(线程安全, 可直接复用)
     * <p>
     * 示例: {@code RestTemplate rt = RestTemplateUtil.init();}
     *
     * @return 预配置的 RestTemplate(超时为默认值或最近一次 {@link #init(int, int)} 设置的值)
     */
    public static RestTemplate init() {
        return REST_TEMPLATE;
    }

    /**
     * 用自定义超时重建共享的 RestTemplate 实例(覆盖默认 5s/30s)
     * <p>
     * 典型场景: Spring 启动时由配置类(如 RestConfig)从 application.yml 读取
     * {@code na.rest.connect-timeout} / {@code na.rest.read-timeout} 后调用本方法,
     * 使静态实例的超时与配置一致。重建后所有静态方法(get/post/execute)将使用新实例。
     * <p>
     * 注意: RestTemplate 无状态, 重建不影响进行中的请求; 但若调用方缓存了
     * {@link #init()} 返回的旧实例引用, 重建后该引用仍指向旧实例(超时不变),
     * 建议通过静态方法调用而非缓存实例引用。
     * <p>
     * 示例:
     * <pre>{@code
     * // application.yml: na.rest.connect-timeout=8000, na.rest.read-timeout=60000
     * RestTemplateUtil.init(8000, 60000);
     * // 之后所有静态调用使用新超时
     * RestTemplateUtil.get("https://api.example.com/hi", String.class);
     * }</pre>
     *
     * @param connectTimeoutMillis 连接超时(毫秒); 传入 <= 0 时沿用默认值 {@value #DEFAULT_CONNECT_TIMEOUT_MILLIS}
     * @param readTimeoutMillis    读取超时(毫秒); 传入 <= 0 时沿用默认值 {@value #DEFAULT_READ_TIMEOUT_MILLIS}
     */
    public static synchronized void init(int connectTimeoutMillis, int readTimeoutMillis) {
        int connect = connectTimeoutMillis > 0 ? connectTimeoutMillis : DEFAULT_CONNECT_TIMEOUT_MILLIS;
        int read = readTimeoutMillis > 0 ? readTimeoutMillis : DEFAULT_READ_TIMEOUT_MILLIS;
        REST_TEMPLATE = createRestTemplate(connect, read);
    }

    /**
     * 发送 GET 请求(无请求头、无参数)
     * <p>
     * 示例:
     * <pre>{@code
     * ResponseEntity<String> resp = RestTemplateUtil.get("https://api.example.com/user/1", String.class);
     * String body = resp.getBody();
     * }</pre>
     *
     * @param url    完整请求地址
     * @param tClass 响应体反序列化类型, 如 String.class / Map.class / 自定义 DTO
     * @param <T>    响应体类型
     * @return 响应实体(含状态码、响应头、响应体)
     */
    public static <T> ResponseEntity<T> get(String url, Class<T> tClass) {
        return REST_TEMPLATE.getForEntity(url, tClass);
    }

    /**
     * 发送 GET 请求(支持自定义请求头与查询参数)
     * <p>
     * 示例:
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Authorization", "Bearer token123");
     * Map<String, Object> params = new HashMap<>();
     * params.put("page", 1);
     * params.put("keyword", "手机"); // 中文与特殊字符会自动 URL 编码
     * ResponseEntity<String> resp = RestTemplateUtil.get(
     *         "https://api.example.com/user/list", headers, params, String.class);
     * }</pre>
     *
     * @param url        请求地址
     * @param headers    请求头, 允许为 null
     * @param parameters 查询参数, 拼接前做 URL 编码, null 值的键会被跳过; 允许为 null
     * @param tClass     响应体反序列化类型
     * @param <T>        响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> get(String url, Map<String, String> headers, Map<String, Object> parameters, Class<T> tClass) {
        // 拼接并编码查询参数
        URI fullUrl = buildUrlWithParams(url, parameters);

        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }

        // GET 请求: 只有请求头, 无请求体
        HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);
        return REST_TEMPLATE.exchange(fullUrl, HttpMethod.GET, httpEntity, tClass);
    }

    /**
     * 发送 POST 请求(JSON 字符串体)
     * <p>
     * 示例:
     * <pre>{@code
     * String json = "{\"name\":\"张三\",\"age\":18}";
     * ResponseEntity<String> resp = RestTemplateUtil.post("https://api.example.com/user", json);
     * }</pre>
     *
     * @param url  请求地址
     * @param body JSON 字符串请求体
     * @return 响应实体(响应体固定为 String)
     */
    public static ResponseEntity<String> post(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * 发送 POST 请求(表单格式, Content-Type: application/x-www-form-urlencoded)
     * <p>
     * 示例:
     * <pre>{@code
     * MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
     * form.add("username", "admin");
     * form.add("password", "123456");
     * ResponseEntity<String> resp = RestTemplateUtil.post(
     *         "https://api.example.com/login", String.class, form);
     * }</pre>
     *
     * @param url    请求地址
     * @param tClass 响应体反序列化类型
     * @param map    表单参数
     * @param <T>    响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> post(String url, Class<T> tClass, MultiValueMap<String, Object> map) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(map, httpHeaders);
        return REST_TEMPLATE.postForEntity(url, httpEntity, tClass);
    }

    /**
     * 发送 POST 请求(表单格式, 普通 Map 参数自动转为表单)
     * <p>
     * 示例:
     * <pre>{@code
     * Map<String, Object> params = new HashMap<>();
     * params.put("grant_type", "authorization_code");
     * params.put("code", "abc123");
     * ResponseEntity<String> resp = RestTemplateUtil.post(
     *         "https://api.example.com/oauth/token", String.class, params);
     * }</pre>
     *
     * @param url    请求地址
     * @param tClass 响应体反序列化类型
     * @param params 表单参数
     * @param <T>    响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> post(String url, Class<T> tClass, Map<String, Object> params) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        params.forEach(multiValueMap::add);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(multiValueMap, httpHeaders);
        return REST_TEMPLATE.postForEntity(url, httpEntity, tClass);
    }

    /**
     * 发送 POST 请求(JSON 对象体, 支持自定义请求头)
     * <p>
     * 示例:
     * <pre>{@code
     * Map<String, Object> body = new HashMap<>();
     * body.put("orderId", "202401001");
     * // headers 传 null 时默认使用 application/json
     * ResponseEntity<String> resp = RestTemplateUtil.post(
     *         "https://api.example.com/order", String.class, body, null);
     * }</pre>
     *
     * @param url     请求地址
     * @param tClass  响应体反序列化类型
     * @param map     JSON 请求体
     * @param headers 请求头; 为 null 时默认 Content-Type: application/json
     * @param <T>     响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> post(String url, Class<T> tClass, Map<String, Object> map, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(map, headers);
        return REST_TEMPLATE.postForEntity(url, httpEntity, tClass);
    }

    /**
     * 发送 POST 请求(JSON 数组体)
     * <p>
     * 示例:
     * <pre>{@code
     * List<Long> ids = Arrays.asList(1L, 2L, 3L);
     * ResponseEntity<String> resp = RestTemplateUtil.post(
     *         "https://api.example.com/user/batch", String.class, ids, null);
     * }</pre>
     *
     * @param url     请求地址
     * @param tClass  响应体反序列化类型
     * @param list    JSON 数组请求体
     * @param headers 请求头; 为 null 时默认 Content-Type: application/json
     * @param <T>     响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> post(String url, Class<T> tClass, List<?> list, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<List<?>> httpEntity = new HttpEntity<>(list, headers);
        return REST_TEMPLATE.postForEntity(url, httpEntity, tClass);
    }

    /**
     * 发送 GET 请求(仅带自定义请求头, 无查询参数)
     * <p>
     * 示例:
     * <pre>{@code
     * HttpHeaders headers = new HttpHeaders();
     * headers.setBearerAuth("token123");
     * ResponseEntity<String> resp = RestTemplateUtil.execute(
     *         "https://api.example.com/userinfo", String.class, headers);
     * }</pre>
     *
     * @param url     请求地址
     * @param tClass  响应体反序列化类型
     * @param headers 请求头
     * @param <T>     响应体类型
     * @return 响应实体
     */
    public static <T> ResponseEntity<T> execute(String url, Class<T> tClass, HttpHeaders headers) {
        // GET 无请求体: 请求头必须放在第二参数, 否则会被当作请求体丢弃(原实现存在此问题)
        HttpEntity<Void> httpEntity = new HttpEntity<>(null, headers);
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, httpEntity, tClass);
    }

    /**
     * 拼接 URL 与查询参数, 并对参数值做 URL 编码(中文、空格、& 等特殊字符安全)
     *
     * @param url        请求地址(可自带查询参数)
     * @param parameters 查询参数, null 值的键被跳过
     * @return 编码后的完整 URI
     */
    private static URI buildUrlWithParams(String url, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return URI.create(url);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        parameters.forEach((key, value) -> {
            // null 值跳过, 避免 "key=null" 这类无效参数
            if (value != null) {
                builder.queryParam(key, value);
            }
        });
        return builder.build().encode().toUri();
    }
}
