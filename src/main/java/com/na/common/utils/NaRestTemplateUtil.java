package com.na.common.utils;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@ApiModel("远程请求处理工具类")
@Component
@Data
public class NaRestTemplateUtil {

    public static <T> ResponseEntity<T> get(String url, Class tClass) {
        RestTemplate restTemplate = init();
        ResponseEntity<T> forEntity = restTemplate.getForEntity(url, tClass);
        return forEntity;
    }

    public static <T> ResponseEntity<String> get(String url, Map<String, String> headers, Map<String, Object> parameters, Class<T> tClass) {
        RestTemplate restTemplate = init();

        // 将参数拼接到URL
        String fullUrl = buildUrlWithParams(url, parameters);

        // 构建 HttpHeaders
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::add);

        // 创建 HttpEntity
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        // 执行 GET 请求
        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, httpEntity, String.class);
        return response;
    }

    // 拼接 URL 和参数
    private static String buildUrlWithParams(String url, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return url;
        }
        StringBuilder urlWithParams = new StringBuilder(url);
        urlWithParams.append("?");
        parameters.forEach((key, value) -> urlWithParams.append(key).append("=").append(value).append("&"));
        return urlWithParams.substring(0, urlWithParams.length() - 1); // 移除最后一个 '&'
    }

    public static <T> ResponseEntity<String> post(String url, String body) {
        // 初始化 RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // 如果是 JSON 请求，设置内容类型

        // 创建请求实体（包含 body 和 headers）
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        // 发送 POST 请求，并返回 ResponseEntity<String>
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        return response; // 返回 ResponseEntity<String>
    }


    public static <T> ResponseEntity<T> post(String url, Class tClass, MultiValueMap<String, Object> map) {
        RestTemplate restTemplate = init();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(map, httpHeaders);
        ResponseEntity<T> openLinkEntityResponseEntity = restTemplate.postForEntity(url, httpEntity, tClass);
        return openLinkEntityResponseEntity;
    }

    public static <T> ResponseEntity<T> post(String url, Class tClass, Map<String, Object> params) {
        RestTemplate restTemplate = init();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 转换为 MultiValueMap
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            multiValueMap.add(entry.getKey(), entry.getValue());
        }
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(multiValueMap, httpHeaders);
        ResponseEntity<T> openLinkEntityResponseEntity = restTemplate.postForEntity(url, httpEntity, tClass);
        return openLinkEntityResponseEntity;
    }

    public static <T> ResponseEntity<T> post(String url, Class tClass, Map<String, Object> map,HttpHeaders headers) {
        RestTemplate restTemplate = init();
        if(headers == null){
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(map, headers);
        ResponseEntity<T> openLinkEntityResponseEntity = restTemplate.postForEntity(url, httpEntity, tClass);
        return openLinkEntityResponseEntity;
    }

    public static <T> ResponseEntity<T> post(String url, Class<T> tClass, List<?> list, HttpHeaders headers) {
        RestTemplate restTemplate = init();
        if (headers == null) {
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<List<?>> httpEntity = new HttpEntity<>(list, headers);
        return restTemplate.postForEntity(url, httpEntity, tClass);
    }

    public static <T> ResponseEntity<String> execute(String url, Class tClass, HttpHeaders headers) {
        RestTemplate restTemplate = init();
        HttpEntity httpEntity = new HttpEntity<>( headers);
        ResponseEntity<String> forEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity,tClass);
        return forEntity;
    }



    public static RestTemplate init(){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(Charset.forName("utf-8")));
        return restTemplate;
    }

}
