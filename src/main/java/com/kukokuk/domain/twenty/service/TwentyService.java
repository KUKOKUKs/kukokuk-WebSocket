package com.kukokuk.domain.twenty.service;

import com.kukokuk.common.dto.ApiResponse;
import com.kukokuk.domain.twenty.dto.RoomUser;
import com.kukokuk.domain.twenty.dto.SendStdMsg;
import com.kukokuk.domain.twenty.util.RedisLockManager;
import com.kukokuk.domain.twenty.vo.TwentyRoom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@RequiredArgsConstructor
public class TwentyService {

    private final SimpMessagingTemplate template;
    private final RedisLockManager redisLockManager;
    private final TaskScheduler taskScheduler;
    private final RestTemplate restTemplate;

    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    /*private final RedisTemplate<Object, String> redisTemplate;*/

    @Value("${api.server.base-url}")
    private String apiBaseUrl;

    /**
     * 게임 시작 기능
     * 1. 시스템 문장을 저장할 변수 생성
     * 2. 게임방 상태 IN_PROGRESS로 변경
     * 3. 현재 상태의 게임방 조회
     * 4. 시스템 문장과 게임방의 상태값을 map에 할당
     * 5. 브로드 캐스팅
     * @param roomNo
     */
    public void gameStart(int roomNo) {
        //API 서버에 게임방 상태 변경 요청
        restTemplate.postForEntity(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS", null,
            Void.class);   //=> 그냥 단순 POST 요청일 때는, 이런 식으로만 코딩하면된다.

        //최신 게임방 정보 조회
        ResponseEntity<ApiResponse<TwentyRoom>> resp = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<TwentyRoom>>() {
            }
        );
        TwentyRoom room = resp.getBody().getData();
        //=> 이렇게 값을 조회해오는 경우는 코딩을 이렇게 해야된다.

        //브로드 캐스팅
        Map<String, Object> map = new HashMap<>();
        map.put("roomStatus", room.getStatus());
        map.put("system", "스무고개를 시작합니다.");
        template.convertAndSend("/topic/gameStart/" + roomNo, map);
    }

    /**
     * 게임방에 입장을 했을 경우,
     * 1. 입장한 사용자의 상태를 JOINED로 변경
     * 2. 이 게임방의 참여자 전체의 리스트를 조회
     * 3. 이 게임방을 조회
     * 4. 참여자 리스트와 게임방을 map에 담아 브로드 캐스팅
     */
    public void joinGameRoom(int userNo, int roomNo) {
        //1. 입장한 유저 상태 변경
        Map<String, Object> req = new HashMap<>();
        req.put("roomNo", roomNo);
        req.put("userNo", userNo);
        req.put("status", "JOINED");
        restTemplate.postForEntity(apiBaseUrl + "/api/twenty/room/user/status", req, Void.class);

        // 2. 최신 참여자 리스트와 방 상태 가져오기
        ResponseEntity<ApiResponse<List<RoomUser>>> resp1 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<List<RoomUser>>>() {
            }
        );
        List<RoomUser> list = resp1.getBody().getData();

        //최신 방 데이터 가져오기
        ResponseEntity<ApiResponse<TwentyRoom>> resp2 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<TwentyRoom>>() {
            }
        );
        TwentyRoom room = resp2.getBody().getData();

        // 3. 브로드캐스팅
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("roomStatus", room.getStatus());
        template.convertAndSend("/topic/participants/" + roomNo, map);
    }

    /**
     * 교사가 끊겼을 경우
     * 1. 게임종료 누른 경우 => 그냥 종료
     * 2. 서버가 팅기거나 웹을 닫을 경우 - 게임방 상태 STOPPED로 변경, 교사 및 모든 학생의 상태 LEFT로 변경
     * - 이 게임방의 전체 유저 조회
     * - 게임방을 조회
     * - map 객체에 담아서 브로드캐스팅(전체 유저 + 게임방 상태)
     *
     * @param roomNo
     */
    public void handleTeacherDisconnect(int roomNo) {
        // 현재 게임방을 조회
        ResponseEntity<ApiResponse<TwentyRoom>> resp1 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<TwentyRoom>>() {
            }
        );
        TwentyRoom room = resp1.getBody().getData();

        Map<String, Object> map = new HashMap<>();
        // 게임방이 이미 종료된 것이라면, 어차피 조회가 안되서 null일 것이다!
        if (room == null) {
            System.out.println("이미 종료된 게임방입니다.");
            return;
        } else {            //그렇다면 그냥 생으로 닫은 경우 이런 식으로 게임방과 사용자의 상태를 변경한다!
            map.put("roomStatus", "STOPPED");
            map.put("status", "LEFT");
            map.put("roomNo", roomNo);
            restTemplate.postForObject(apiBaseUrl + "/api/twenty/room/disconnect/teacher",
                map, Void.class);
        }
        String payLoad = "정상적으로 연결을 끊습니다.";
        taskScheduler.schedule(
            () -> {
                try {
                    template.convertAndSend("/topic/TeacherDisconnect", payLoad);
                    System.out.println("✅ 교사 종료 이벤트 전송 완료");
                } catch (Exception e) {
                    System.err.println("⚠️ 전송 실패: " + e.getMessage());
                }
            },
            Instant.now().plusMillis(500) // 500ms (0.5초) 지연
        );
    }

    /**
     * 학생이 끊겼을 경우 웹 브라우저 탭을 닫을 경우,
     * 1. 이 학생만 상태를 LEFT로 변경
     * 2. 최신 전체 유저 리스트를 조회
     * 3. 이 리스트를 다시 브로드 캐스팅
     * @param roomNo
     * @param userNo
     * @return
     */
    public void handleStudentDisconnect(int roomNo, int userNo) {
        Map<String, Object> map = new HashMap<>();
        map.put("roomNo", roomNo);
        map.put("userNo", userNo);
        map.put("status", "LEFT");
        restTemplate.postForEntity(apiBaseUrl + "/api/twenty/room/user/status", map, Void.class);
        map.clear();

        ResponseEntity<ApiResponse<List<RoomUser>>> resp2 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<List<RoomUser>>>() {
            }
        );
        List<RoomUser> list = resp2.getBody().getData();
        map.put("list", list);
        template.convertAndSend("/topic/participants/" + roomNo, map);
    }

    /**
     * 손들기 버튼 기능
     * 1. AWAITING_INPUT으로 방 상태 변경.
     * 2. 40초 서버 타이머 부여.
     * 3. 현재 게임방의 메세지 개수를 조회.
     *  - 19개 이상이라면, 경고 메세지 map 객체에 할당
     * 4. 그 외 필수 데이터 map 객체에 할당
     * 5. 브로드 캐스팅
     * @param roomNo
     * @param userNo
     */
    public void raiseHand(int roomNo, int userNo, String userNickName) {
        if (redisLockManager.trySetQuestioner(roomNo, userNo)) {

            // AWAITING_INPUT으로 방 상태 변경
            restTemplate.postForEntity(
                apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=AWAITING_INPUT",
                null, Void.class);

            // 서버에서 40초 제한 시간 부여
            ScheduledFuture<?> scheduledFuture =
                taskScheduler.schedule(() -> turnTimeout(roomNo), Instant.now().plusSeconds(40));
            scheduledTasks.put(roomNo, scheduledFuture);

            //브로드캐스팅 시 필수로 보내야하는 값을 먼저 담기
            Map<String, Object> map = new HashMap<>();

            // 현재 게임방의 질문&정답 횟수를 조회.
            ResponseEntity<ApiResponse<Integer>> resp = restTemplate.exchange(
                apiBaseUrl + "/api/twenty/room/" + roomNo + "/msgCnt",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Integer>>() {
                }
            );
            Integer msgCnt = resp.getBody().getData();
            //질문 횟수가 19개 이상이라면
            if (msgCnt >= 19) {
                map.put("system", "정답을 입력해주세요");
                map.put("msgCnt", msgCnt);
            }

            //필수로 보내야하는 데이터
            map.put("roomStatus", "AWAITING_INPUT");
            map.put("userNo", userNo);
            map.put("name", userNickName);
            map.put("time", 40);

            // 브로드캐스팅
            template.convertAndSend("/topic/raisehand/" + roomNo, map);
        }
    }

    /**
     * 40초 제한 시간안에 답변을 제출하지 못한 경우
     * 1.Redis에 저장된 1등으로 선별된 유저를 먼저 초기화
     * 2.게임방을 조회해서, AWAITING_INPUT 인지 확인.
     * 3.IN_PROGRESS로 변경
     * 4.system 메세지 설정, 이 방의 상태값을 map에 담아 브로드캐스팅
     * 5.40초 제한시간 해제
     */
    public void turnTimeout(int roomNo) {
        redisLockManager.releaseQuestionerLock(roomNo); //분산 락 초기화
        //게임방 조회
        ResponseEntity<ApiResponse<TwentyRoom>> resp = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo,
            HttpMethod.GET,
            null,
            new  ParameterizedTypeReference<ApiResponse<TwentyRoom>>() {}
        );
        TwentyRoom room = resp.getBody().getData();

        //게임방 상태 확인& 브로드캐스팅
        if ("AWAITING_INPUT".equals(room.getStatus())) {
            restTemplate.postForEntity(
                apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS",
                null, Void.class);

            Map<String, Object> map = new HashMap<>();
            map.put("roomStatus", "IN_PROGRESS");
            template.convertAndSend("/topic/turnTimeout/" + roomNo, map);
        }

        //타이머 초기화
        scheduledTasks.remove(roomNo);
    }


    /**
     * 학생이 40초 안에 질문 또는 답변을 제출했을 경우
     * 1. 먼저 타이머 취소.
     * 2. Redis에 저장된 1등 학생을 삭제.
     * 3.게임방의 상태를 "AWAITING_RESPONSE"로 변경
     * 4.그대로 msg 데이터를 log 테이블에 inert
     * 5.다시 이 게임방의 전체 메세지 수 갱신
     * 6.msg에 담긴 내용, 방의 상태를 map에 담아 브로드 캐스팅.
     * @param msg
     */
    public void sendStdMsg(SendStdMsg msg, String nickName) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(msg.getRoomNo());

        //타이머 삭제
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(msg.getRoomNo());
        }

        //분산 락 초기화
        redisLockManager.releaseQuestionerLock(msg.getRoomNo());

        //게임방 상태 변경 -> 교사 답변 대기 중으로 변경.
        restTemplate.postForEntity(
            apiBaseUrl + "/api/twenty/room/" + msg.getRoomNo() + "/status?status=AWAITING_RESPONSE",
            null, Void.class);

        //msg 데이터 log 테이블에 할당하기
        //body에 msg를 담아서 보내는 것으로 한다.
        ResponseEntity<ApiResponse<SendStdMsg>> resp = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/saveLog",
            HttpMethod.POST,
            new HttpEntity<>(msg),
            new ParameterizedTypeReference<ApiResponse<SendStdMsg>>() {
            }
        );
        SendStdMsg currentMsg = resp.getBody().getData();

        // 전체 메세지 개수 조회.
        ResponseEntity<ApiResponse<Integer>> resp2 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + msg.getRoomNo() + "/msgCnt",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<Integer>>() {
            }
        );
        Integer msgCnt = resp2.getBody().getData();

        //이후 필요한 값만 정해서 브로드 캐스팅
        Map<String, Object> map = new HashMap<>();
        map.put("logNo", currentMsg.getLogNo());
        map.put("userNo", currentMsg.getUserNo());
        map.put("msgType", currentMsg.getType());
        map.put("content", currentMsg.getContent());
        map.put("nickName", nickName);
        map.put("msgCnt",msgCnt);
        map.put("roomStatus", "AWAITING_RESPONSE");

        //실시간 채팅 메세지를 화면에 실시간으로 보여주기 위해 브로드 캐스팅 실행.
        template.convertAndSend("/topic/sendStdMsg/" + msg.getRoomNo(), map);
    }

    /**
     * 교사가 O&X 버튼을 눌렀을 경우, 학생의 질문&정답에 따라 처리를 달리 한다.
     * 1. 게임방의 상태를 우선 IN_PROGRESS로 변경
     * 2 .roomNo로 이 게임방의 가장 최신 메세지 1개를 조회(logNo, type,userNo, content,cnt)
     *  - cnt: 이 메세지가 몇 번째 메세지인지 - 정수 값
     * 3. 메세지의 타입에 따라, log테이블를 업데이트 및 브로드 캐스팅
     * @param map roomNo,response
     */
    public void teacherResponse(Map<String, Object> map) {
        // 게임방 상태 IN_PROGRESS로 변경
        Integer roomNo =  (Integer) map.get("roomNo");
        restTemplate.postForEntity(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS",
            null, Void.class);

        //브로드 캐스팅할 값 가공
        Map<String, Object> map2 = new HashMap<>();
        map2.put("roomStatus", "IN_PROGRESS");
        List<SendStdMsg> msgList = new ArrayList<>();
        String response = map.get("response").toString(); // 교사가 보낸 O or X

        // 가장 최신 메세지 조회
        ResponseEntity<ApiResponse<SendStdMsg>> resp2 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/msg/recent",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<SendStdMsg>>() {}
        );
        SendStdMsg recentMsg = resp2.getBody().getData();
        // recentMsg = logNo, type,userNo,content, cnt, roomNo

        //가장 최신 메세지의 타입에 따라서, 교사의 응답에 따른 Update 작업.
        if("N".equals(response) && recentMsg != null && "Q".equals(recentMsg.getType())) {          //질문에 X한 경우
            // log 테이블에 업데이트할 메세지의 내용과 질문 응답을 설정
            recentMsg.setContent(recentMsg.getContent() + " :❌");
            recentMsg.setAnswer("N");

        }else if("N".equals(response) && recentMsg != null && "A".equals(recentMsg.getType())) {    //정답에 X한 경우
            // log 테이블에 업데이트할 메세지의 내용과 정답응답을 설정
            recentMsg.setContent(recentMsg.getContent() + " :❌");
            recentMsg.setIsSuccess("N");

            //이 메세지가 20번째 이상이라면, 게임이 끝난 상황이므로
            if(recentMsg.getCnt() >= 20) {
                // 브로드 캐스팅할 시스템 메세지를 하나 만들고,
                map2.put("system", "정답을 맞추지 못했습니다.. 선생님은 스무고개를 종료해주세요..");
                //이 게임방의 결과를 DB에 REST로 요청하여 변경한다.
                TwentyRoom room = new TwentyRoom();
                room.setRoomNo(roomNo);
                room.setIsSuccess("N");
                room.setTryCnt(recentMsg.getCnt());
                restTemplate.postForEntity(apiBaseUrl + "/api/twent/room" + roomNo + "/resultUpdate",
                                              room,
                                              Void.class);
            }
            // 그게 아닌 경우, 아무 설정할 것이 읎다!

        }else if("Y".equals(response) && recentMsg != null && "Q".equals(recentMsg.getType())) {    //질문에 O한 경우
            // log 테이블에 업데이트할 메세지의 내용과 질문 응답을 설정
            recentMsg.setContent(recentMsg.getContent() + " :⭕");
            recentMsg.setAnswer("Y");

        }else if("Y".equals(response) && recentMsg != null && "A".equals(recentMsg.getType())) {    //정답에 O한 경우
            // log 테이블에 업데이트할 메세지의 내용과 정답 응답을 설정
            recentMsg.setContent(recentMsg.getContent() + " :⭕");
            recentMsg.setIsSuccess("Y");

            //정답을 맞춘 경우로, 브로드캐스팅할 시스템 메세지를 만들고
            map2.put("system","정답을 맞추셨습니다.!!");

            // 게임방의 결과를 업데이트 한다.!
            TwentyRoom room = new TwentyRoom();
            room.setRoomNo(roomNo);
            room.setIsSuccess("Y");
            room.setTryCnt(recentMsg.getCnt());
            room.setWinnerNo(recentMsg.getUserNo());
            restTemplate.postForEntity(apiBaseUrl + "/api/twent/room" + roomNo + "/resultUpdate",
                                         room,
                                         Void.class);
        }

        //위 조건문에서 새롭게 설정한 recentMsg를 업데이트하고, 가장 최신의 메세지 리스트를 반환 받는다!
        ResponseEntity<ApiResponse<List<SendStdMsg>>> resp3 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/updateMsgLog",
            HttpMethod.POST,
            new HttpEntity<>(recentMsg),
            new  ParameterizedTypeReference<ApiResponse<List<SendStdMsg>>>() {}
        );
        msgList = resp3.getBody().getData();
        map2.put("msgList", msgList);

        //브로드 캐스팅
        template.convertAndSend("/topic/TeacherResponce", map2);
        //map2 : msgList(logNo, userNo, nickName, type, content), roomStatus,system(게임이 끝날 때만)
    }
}
