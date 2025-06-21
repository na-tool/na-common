package com.na.common.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(
        name = {"na.rest"},
        matchIfMissing = false
)
public class NaRestConfig {
    @Bean
    @LoadBalanced  // 启用负载均衡
    // 定义一个名为 restTemplate 的 Bean
    public RestTemplate restTemplate() {
        // 创建一个 SimpleClientHttpRequestFactory 实例
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间为 60 秒
        requestFactory.setConnectTimeout(60000);
        // 设置读取超时时间为 60 秒
        requestFactory.setReadTimeout(60000);
        /* 防止中文乱码 */
        // 创建一个 RestTemplate 实例并设置请求工厂
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // 清空默认的消息转换器列表
        restTemplate.getMessageConverters().clear();
        // 添加 FastJsonHttpMessageConverter 作为消息转换器
        restTemplate.getMessageConverters().add(new FastJsonHttpMessageConverter());
        // 返回配置好的 RestTemplate 实例
        return restTemplate;
    }
}
