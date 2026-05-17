package com.payshield.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * REST Client configuration for calling the Python AI microservice
 */
@Configuration
public class RestClientConfig {

    @Value("${payshield.ai.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${payshield.ai.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
