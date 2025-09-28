package com.kukokuk.domain.twenty.util;

import com.kukokuk.domain.twenty.service.TwentyService;
import com.kukokuk.security.SecurityUser;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final TwentyService twentyService;

    @EventListener
    public void handleDisconnectRoom(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        Integer roomNo = (Integer) accessor.getSessionAttributes().get("currentRoomNo");

        if (principal instanceof Authentication auth) {
            SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
            List<String> role = securityUser.getUser().getRoleNames();
            int userNo = securityUser.getUser().getUserNo();

            //교사가 게임방을 나갔을 경우,
            if (role.contains("ROLE_TEACHER") && roomNo != null) {
                twentyService.handleTeacherDisconnect(roomNo);
            } else { // 학생이 나갔을 경우,
                twentyService.handleStudentDisconnect(roomNo, userNo);
            }
        }

    }
}
