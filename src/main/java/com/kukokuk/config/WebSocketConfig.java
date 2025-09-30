package com.kukokuk.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.Authentication;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${api.server.base-url}")
    private String baseUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins(baseUrl) // "http://localhost:8080"
            .setHandshakeHandler(new DefaultHandshakeHandler() {
                @Override
                protected Principal determineUser(ServerHttpRequest request,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {
                    // Servlet request로 변환 가능하면 쿠키/세션 조사
                    if (request instanceof ServletServerHttpRequest servletReq) {
                        HttpServletRequest servlet = servletReq.getServletRequest();

                        // 1) HttpSession에서 SecurityContext 추출(있다면 Principal로 반환)
                        var httpSession = servlet.getSession(false);
                        if (httpSession != null) {
                            Object sc = httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
                            if (sc instanceof SecurityContext securityContext) {
                                Authentication auth = securityContext.getAuthentication();
                                if (auth != null && auth.isAuthenticated()) {
                                    // 세션 id 저장(나중에 RestTemplate 요청에 사용)
                                    attributes.put("JSESSIONID", httpSession.getId());
                                    return auth; // Principal로 Authentication 반환 가능
                                }
                            }
                        }

                        // 2) 세션이 없거나 다른 경우 쿠키에서 JSESSIONID 추출
                        Cookie[] cookies = servlet.getCookies();
                        if (cookies != null) {
                            for (Cookie c : cookies) {
                                if ("JSESSIONID".equals(c.getName())) {
                                    attributes.put("JSESSIONID", c.getValue());
                                    break;
                                }
                            }
                        }
                    }

                    // fallback: 기본 Principal (없다면 null)
                    return request.getPrincipal();
                }
            })
            .withSockJS(); // SockJS 사용중이면 유지
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
