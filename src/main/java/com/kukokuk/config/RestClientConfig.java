package com.kukokuk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * 서비스 단에서 DB 작업을 할 때, REST API 요청을 하는 쉽게 하기 위한 도구
     * @return RestTemplate를 반환 -> REST API 사용을 더 쉽게 하도록 함.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

