package com.kukokuk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class HttpSessionConfig {
    /**
     * - 원래 HttpSession을 톰캣 메모리에 저장되는 것을 Redis에 저장하게 해주는 코드임.
     * - 그래서 @AuthenticationPrincipal에서 꺼내던 것들은 그대로 사용하면됨.
     * - 웹소켓의 경우, Principal로 꺼내면 됨.
     * - 그냥 세션이 저장되는 위치만 Redis로 변경되는 것임.
     */

    /** 과정
     * 사용자 로그인 -> SecurityUser가 HttpSession에 들어감. -> 이 내용을 Redis에 저장. ->
     * 브라우저가 SockJS("웹소켓 주소")로 WebSocket 연결 요청을 보낼 때, 쿠키(JSESSIONID)도 같이 전달.
     * -> Redis 세션 안에는 SecurityContext - Authentication - Principal(SecurityUser)를 복원 ->
     * WebSocket 핸들러에서 Principal 객체로 접근.
     */
}

