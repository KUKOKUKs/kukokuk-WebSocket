package com.kukokuk.domain.twenty.controller;

import com.kukokuk.common.util.FilePathUtil;
import com.kukokuk.domain.twenty.dto.SendStdMsg;
import com.kukokuk.domain.twenty.service.TwentyService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TwentyController {
    private final TwentyService twentyService;

    /**
     *  학생 또는 교사가 게임에 입장했을 때
     * @param currentRoomNo 현재 게임방 식별자 번호
     * @param simpMessageHeaderAccessor 사용자 정보
     *  ++@DestinationVariable
     *  ++SimpMessageHeaderAccessor : Spring 내부에서 관리하는
     */
    @MessageMapping("/join/{currentRoomNo}")
    public void joinGameRoom(@DestinationVariable int currentRoomNo,
                                                  SimpMessageHeaderAccessor simpMessageHeaderAccessor) {
        // 입장하자마 세션에 현재 게임방 식별자 번호를 집어 넣음. -> 이 사용자가 서버가 끊겼을 경우 이벤트에 사용하기 위해서
        simpMessageHeaderAccessor.getSessionAttributes().put("currentRoomNo", currentRoomNo);

        Integer userNo = (Integer) simpMessageHeaderAccessor.getSessionAttributes().get("userNo");
        String nickName =  (String) simpMessageHeaderAccessor.getSessionAttributes().get("nickName");

        if(userNo == null) {
            System.out.println("웹소켓 세션에 userNo가 존재하지 않음.");
            return;
        }
        if(nickName == null) {
            System.out.println("웹소켓 세션에 nickName이 존재하지 않음.");
            nickName = "unknown";
            return ;
        }
        twentyService.joinGameRoom(userNo, currentRoomNo);

    }

    /**
     * 교사가 게임 시작 버튼을 눌렀을 때!
     * @param currentRoomNo
     */
    @MessageMapping("/gameStart/{currentRoomNo}")
    public void gameStart(@DestinationVariable int currentRoomNo) {
        twentyService.gameStart(currentRoomNo);
    }

    /**
     * 학생이 손들기 버튼을 눌렀을 때.
     * @param currentRoomNo 현재 게임방 식별자 번호
     * @param accessor 사용자 정보
     */
    @MessageMapping("/raisehand/{currentRoomNo}")
    public void raiseHand(@DestinationVariable int currentRoomNo, SimpMessageHeaderAccessor accessor) {
        Integer userNo = (Integer) accessor.getSessionAttributes().get("userNo");
        String nickName = (String) accessor.getSessionAttributes().get("nickName");
        if(userNo == null) {
            System.out.println("웹소켓 세션에 userNo가 존재하지 않음.");
            return;
        }
        if(nickName == null) {
            System.out.println("웹소켓 세션에 nickName이 존재하지 않음.");
            nickName = "unknown";
            return;
        }
        twentyService.raiseHand(currentRoomNo, userNo, nickName);
    }

    /**
     * 학생이 질문 또는 메세지를 보냈을 때
     * @param msg: 클라이언트에서 보낸 메세지 1개의 데이터
     */
    @MessageMapping("/sendStdMsg")
    public void sendStdMsg(@Payload SendStdMsg msg, SimpMessageHeaderAccessor accessor) {
        if(accessor == null) {
            System.out.println("사용자 정보가 없습니다.");
            return;
        }
        String nickName = (String) accessor.getSessionAttributes().get("nickName");
        String profileFilename = (String)accessor.getSessionAttributes().get("profileFilename");
        Integer userNo = (Integer) accessor.getSessionAttributes().get("userNo");
        twentyService.sendStdMsg(msg,nickName, FilePathUtil.getProfileImagePath(userNo, profileFilename));
    }

    /**
     * 교사가 O&X 버튼을 눌렀을 때,
     * 학생의 질문 또는 정답의 O,X 처리
     * @param map roomNo,response
     */
    @MessageMapping("/teacherResponse")
    public void teacherResponse (@Payload Map<String,Object> map){
        twentyService.teacherResponse(map);
    }
}
