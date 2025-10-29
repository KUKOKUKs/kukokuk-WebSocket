package com.kukokuk.domain.twenty.service;

import com.kukokuk.common.dto.ApiResponse;
import com.kukokuk.domain.twenty.dto.RoomUser;
import com.kukokuk.domain.twenty.dto.SendStdMsg;
import com.kukokuk.domain.twenty.util.RedisLockManager;
import com.kukokuk.domain.twenty.vo.TwentyRoom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
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

        // 최신 참여자 리스트 조회
        ResponseEntity<ApiResponse<List<RoomUser>>> resp1 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<List<RoomUser>>>() {
            }
        );
        List<RoomUser> list = resp1.getBody().getData();

        //최신 방 데이터 조회
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
     * @param roomNo
     */
    public void handleTeacherDisconnect(int roomNo) {

        // 현재 게임방을 조회
        ResponseEntity<ApiResponse<TwentyRoom>> resp1 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/all/" + roomNo,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<TwentyRoom>>() {
            }
        );
        TwentyRoom room = resp1.getBody().getData();

        Map<String, Object> map = new HashMap<>();

        //이 게임방의 상태가 COMPLETED일 경우 브로드 캐스팅
        if ("COMPLETED".equals(room.getStatus())) {
            return;
        } else { //그 외의 경우 게임을 일부러 중단한 상황이므로, 게임방 상태를 변경 -> 브로드 캐스팅
            map.put("roomStatus", "STOPPED");
            map.put("status", "LEFT");
            map.put("roomNo", roomNo);
            restTemplate.postForObject(apiBaseUrl + "/api/twenty/room/disconnect/teacher",
                map, Void.class);
            String payLoad = "연결을 끊습니다.";
            broadDelay("/topic/TeacherDisconnect", payLoad,500);
            log.info("게임 중단,브로드캐스팅,STOPPED");
        }
    }

    /**
     * 웹소켓 서버가 끊긴 상황에 로직 수행할 때, 서버 끊김을 잠시 딜레이를 걸어 정상적으로 브로드 캐스팅하기 위한 메소드
     * @param topic 브로드 캐스팅 주소
     * @param data 브로드 캐스팅 시, 보내야하는 데이터
     * @param delay 딜레이 시간
     */
    public void broadDelay(String topic, Object data, long delay) {
        taskScheduler.schedule(
            () -> {
                try {
                    template.convertAndSend(topic,data);
                }catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            },
            Instant.now().plusMillis(delay)
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
     * 1. 사용자가 손들기 버튼을 누르면, 먼저 이 사용자가 가장 먼저 누른 사용자인지 확인.
     * 2. true라면, 게임방 상태 : AWAITING_INPUT으로 변경 -> 40초 제한시간 부여 -> userNo,nickName,time, roomStatus를 map 객체에 할당.
     * 3. 여기서 질문 개수를 한번 조회해서, 19개 이상일 경우 시스템 메세지를 만들어 map 객체에 할당
     * 4. 이후 브로드 캐스팅
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
     * 교사가 O,X 버튼을 눌렀을 경우 처리
     * 1. map 안의 roomNo, reponse를 꺼낸다.
     * 2. 게임방의 상태를 IN_PROGRESS로 변경한다.
     * 3. 가장 최신 메세지 1개를 반환. (logNo, userNo, type, content, cnt, roomNo)
     * 4. 먼저 최신 메세지 내용을 교사 응답과 합치기
     * 5. 학생의 질문 타입에 따라 setAnswer(교사 응답), setIsSuccess(교사응답)을 채운다.
     *    - 근데 정답에 O를 했거나, 질문 횟수가 20일 때는 게임 종료 신호를 보낸다.
     * 6. 최신 메세지를 DB에 저장 및 전체 리스트를 반환 하여, 브로드 캐스팅
     * @param map roomNo,response
     */
    public void teacherResponse(Map<String, Object> map) {
        // map 안의 값을 꺼낸다.
        int roomNo =  Integer.parseInt(map.get("roomNo").toString());
        String response = map.get("response").toString();    //교사 응답

        // 게임방의 상태를 변경한다.
        restTemplate.postForEntity(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS",
            null, Void.class);
        // 가장 최신 메세지 1개를 반환
        ResponseEntity<ApiResponse<SendStdMsg>> resp2 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/msg/recent",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<SendStdMsg>>() {}
        );
        SendStdMsg recentMsg = resp2.getBody().getData();
        // recentMsg = logNo, type,userNo,content, cnt, roomNo
        log.info("recentmsg: {}",  recentMsg.getLogNo());

        // 교사의 응답에 따라 최신 메세지 내용 업데이트(메세지 타입 상관 없이)
        if("Y".equals(response)) {                                  //교사 응답 O
            recentMsg.setContent(recentMsg.getContent() + " :⭕");
        }else {                                                     //교사 응답 X
            recentMsg.setContent(recentMsg.getContent() + " :❌");
        }

        // 사용자 메세지에 교사 응답 집어넣기.(메세지에 대한 응답이니까)
        recentMsg.setAnswer(response);

        Map<String,Object> map2 = new HashMap<>();

        // 메세지가 정답 타입일 때, 성공 여부에도 교사 응답을 집어 넣는다.
        if("A".equals(recentMsg.getType())) {
            recentMsg.setIsSuccess(response);

            // 정답 타입일 때, 교사 응답이 Y 이거나, 총 메세지 개수가 20개 이상일 때는 게임이 끝난 상황
            // 따라서 결과를 저장.
            if("Y".equals(response) || recentMsg.getCnt() >= 20) {
                map2.put("var","스무고개 종료."); // 이건 종료되었다는 신호를 보낼려고 넣는 변수
                map2.put("teacherResponse",response); // 정답 응답에 따라, 안내 메세지를 다르게 하려고 넣는 거
                log.info("recentmsg : {}", recentMsg);

                //최근 메세지를 던져서, 게임방의 결과를 저장.
                restTemplate.postForObject(apiBaseUrl + "/api/twenty/room/gameOver",
                    recentMsg, Void.class);
            }
        }
        //이제 가공된 recentMsg를 가지고 log 테이블 업데이트 및 전체 메세지 리스트를 반환
        ResponseEntity<ApiResponse<List<SendStdMsg>>> resp3 = restTemplate.exchange(
            apiBaseUrl + "/api/twenty/updateMsgLog",
            HttpMethod.POST,
            new HttpEntity<>(recentMsg),
            new  ParameterizedTypeReference<ApiResponse<List<SendStdMsg>>>() {}
        );
        List<SendStdMsg> msgList = resp3.getBody().getData();
        map2.put("roomStatus", "IN_PROGRESS");
        map2.put("msgList", msgList);
        template.convertAndSend("/topic/TeacherResponce", map2);
    }
}
