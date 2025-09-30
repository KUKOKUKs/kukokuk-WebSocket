package com.kukokuk.common.util;

import com.kukokuk.common.dto.ApiResponse;
import java.net.URI;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class RestTemplateUtils {

    // GET 요청 (with JSESSIONID)
    public static <T> T get(RestTemplate restTemplate, String url, String jsessionId, ParameterizedTypeReference<ApiResponse<T>> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "JSESSIONID=" + jsessionId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            URI.create(url),
            HttpMethod.GET,
            entity,
            typeRef
        );

        return response.getBody().getData();
    }

    // POST 요청 (with body, JSESSIONID)
    public static <T> T post(RestTemplate restTemplate, String url, Object body, String jsessionId, ParameterizedTypeReference<ApiResponse<T>> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "JSESSIONID=" + jsessionId);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            URI.create(url),
            HttpMethod.POST,
            entity,
            typeRef
        );

        return response.getBody().getData();
    }

    // POST 요청 (응답 데이터 필요 없는 경우)
    public static void postVoid(RestTemplate restTemplate, String url, Object body, String jsessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "JSESSIONID=" + jsessionId);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
            URI.create(url),
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
    }
}
