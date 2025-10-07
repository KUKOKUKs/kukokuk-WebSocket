package com.kukokuk.config;

import com.kukokuk.domain.twenty.dto.WsUser;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${api.server.base-url}")
    private String baseUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(baseUrl)
            .setHandshakeHandler(new DefaultHandshakeHandler() {
                @Override
                protected Principal determineUser(ServerHttpRequest request,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {

                    if (request instanceof ServletServerHttpRequest servletReq) {
                        HttpServletRequest servlet = servletReq.getServletRequest();

                        // 1️⃣ 토큰 추출
                        String token = servlet.getParameter("token");
                        if (token == null || token.isBlank()) {
                            System.out.println("⚠️ token 파라미터 없음");
                            return () -> "anonymous";
                        }

                        String redisKey = "ws:token:" + token;

                        // 2️⃣ Redis에서 DTO 조회
                        WsUser wsUser = (WsUser) redisTemplate.opsForValue().get(redisKey);
                        if (wsUser == null) {
                            System.out.println("⚠️ Redis에 해당 토큰 없음: " + token);
                            return () -> "anonymous";
                        }

                        // 3️⃣ TTL 갱신
                        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);

                        // 4️⃣ 인증 정보 출력
                        System.out.printf("✅ Access Token 인증 성공: userNo=%d, nick=%s%n",
                            wsUser.getUserNo(), wsUser.getNickName());

                        // 5️⃣ 세션에 사용자 정보 저장
                        attributes.put("token", token);
                        attributes.put("userNo", wsUser.getUserNo());
                        attributes.put("nickName", wsUser.getNickName());
                        attributes.put("role", wsUser.getRoleNames());

                        // 6️⃣ Principal 반환
                        return () -> String.valueOf(wsUser.getUserNo());
                    }

                    return () -> "anonymous";
                }
            })
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
