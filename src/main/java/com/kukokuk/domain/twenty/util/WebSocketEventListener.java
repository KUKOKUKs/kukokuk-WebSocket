package com.kukokuk.domain.twenty.util;

import com.kukokuk.domain.twenty.service.TwentyService;
import com.kukokuk.security.SecurityUser;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
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
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        accessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
        Integer roomNo = (Integer) accessor.getSessionAttributes().get("currentRoomNo");
        Integer userNo = (Integer) accessor.getSessionAttributes().get("userNo");
        List<String> role =  (List<String>) accessor.getSessionAttributes().get("role");

        if(roomNo == null || userNo == null) {
            System.out.println("웹소켓에 userNo : " + userNo + ", roomNo: " + roomNo + " 가 존재하지 않음.");
            return ;
        }
        if(role == null) {
            System.out.println("웹소켓 세션에 권한정보가 존재하지 않음.");
            return;
        }
        if(role.contains("ROLE_TEACHER")){
            twentyService.handleTeacherDisconnect(roomNo);
        }else {
            twentyService.handleStudentDisconnect(roomNo,userNo);
        }
    }
   /* StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
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
    }*/
}
