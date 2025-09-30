package com.kukokuk.domain.twenty.controller;

import com.kukokuk.domain.twenty.service.TwentyService;
import com.kukokuk.security.SecurityUser;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TwentyController {
    private final TwentyService twentyService;
    /**
     * - 학생이 게임방에 입장했을 대
     * @param currentRoomNo
     * @param principal     => 웹소켓 서버에서는 사용자 정보를 이 객체를 통해 꺼내야 한다.
     * @parma accessor => 웹소켓 세션에 게임방 No를 담기 위해.. => 그래야 서버 끊길 때 이 게임방No를 활용할 수 있음.
     * <p>
     * 추가로 Http에서는 @PathVariabel을 사용하지만, 웹소켓 서버에서는 @DestinationVariable을 사용
     */
    @MessageMapping("/join/{currentRoomNo}")
    public void joinGameRoom(@DestinationVariable int currentRoomNo, Principal principal,
        StompHeaderAccessor accessor) {
        accessor.getSessionAttributes().put("currentRoomNo", currentRoomNo);
        String jsessionId = (String)accessor.getSessionAttributes().get("JSESSIONID");

        // 웹소켓 세션으로 핸드쉐이킹한 로그인 사용자 정보를 다시 SecurityUser로 변경해서 로직 수행
        if (principal instanceof Authentication auth) {
            SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
            int currentUserNo = securityUser.getUser().getUserNo();
            /*twentyService.joinGameRoom(currentUserNo, currentRoomNo,jsessionId);*/
            twentyService.joinGameRoom(currentUserNo, currentRoomNo);
        }
    }

    /**
     * 교사가 게임 시작 버튼을 눌렀을 때!
     *
     * @param currentRoomNo
     * @param principal
     */
    @MessageMapping("/gameStart/{currentRoomNo}")
    public void gameStart(@DestinationVariable int currentRoomNo, Principal principal, StompHeaderAccessor accessor) {
        String jssessionId = (String)accessor.getSessionAttributes().get("JSESSIONID");
        /*twentyService.gameStart(currentRoomNo,jssessionId);*/
        twentyService.gameStart(currentRoomNo);
    }

    /**
     * 손들기 버튼을 눌렀을 때,
     *
     * @param currentRoomNo
     * @param principal
     */
    @MessageMapping("/raisehand/{currentRoomNo}")
    public void raiseHand(@DestinationVariable int currentRoomNo, Principal principal, StompHeaderAccessor accessor) {
        String  jssessionId = (String)accessor.getSessionAttributes().get("JSESSIONID");
        if (principal instanceof Authentication auth) {
            SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
            int userNo = securityUser.getUser().getUserNo();
            String userNickName = securityUser.getUser().getNickname();
            /*twentyService.raiseHand(currentRoomNo, userNo, userNickName,jssessionId);*/
            twentyService.raiseHand(currentRoomNo, userNo, userNickName);
        }

    }

}
