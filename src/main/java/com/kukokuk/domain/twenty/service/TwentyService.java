package com.kukokuk.domain.twenty.service;

import com.kukokuk.domain.twenty.dto.RoomUser;
import com.kukokuk.domain.twenty.util.RedisLockManager;
import com.kukokuk.domain.twenty.vo.TwentyRoom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${api.server.base-url}")
    private String apiBaseUrl;

    /**
     * 1. 시스템 문장을 저장할 변수 생성
     * 2. 게임방 상태 IN_PROGRESS로 변경
     * 3. 현재 상태의 게임방 조회
     * 4. 시스템 문장과 게임방의 상태값을 map에 담아 브로드 캐스팅
     * @param roomNo
     */
    public void gameStart(int roomNo) {
        //API 서버에 게임방 상태 변경 요청
        restTemplate.postForEntity(
            apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS", null,
            Void.class);

        //최신 게임방 정보 조회
        TwentyRoom room = restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo,
            TwentyRoom.class);

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
     * 3. 이 게임방을 조회 4. 참여자 리스트와 게임방을 map에 담아 브로드 캐스팅
     *
     * @param userNo
     * @param roomNo
     */
    public void joinGameRoom(int userNo, int roomNo) {
        //1. 입장한 유저 상태 변경
        Map<String, Object> req = new HashMap<>();
        req.put("roomNo", roomNo);
        req.put("userNo", userNo);
        req.put("status", "JOINED");
        restTemplate.postForEntity(apiBaseUrl + "/api/twenty/room/user/status", req, Void.class);

        // 2. 최신 참여자 리스트와 방 상태 가져오기
        List<RoomUser> list = Arrays.asList(
            restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
                RoomUser[].class)
        );
        TwentyRoom room = restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo,
            TwentyRoom.class);

        // 3. 브로드캐스팅
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("roomStatus", room.getStatus());
        template.convertAndSend("/topic/participants/" + roomNo, map);
    }

    /**
     * 교사가 끊겼을 경우
     * 1. 게임종료 누른 경우 => 그냥 종료
     * 2. 서버가 팅기거나 웹을 닫을 경우
     * - 게임방 상태 STOPPED로 변경, 교사 및 모든 학생의 상태 LEFT로 변경
     * - 이 게임방의 전체 유저 조회
     * - 게임방을 조회
     * - map 객체에 담아서 브로드캐스팅(전체 유저 + 게임방 상태)
     *
     * @param roomNo
     */
    public void handleTeacherDisconnect(int roomNo) {
        TwentyRoom room = restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo,
            TwentyRoom.class);
        Map<String, Object> map = new HashMap<>();
        if (room == null) {
            System.out.println("이미 종료된 게임방입니다.");
            return;
        } else {    //서버 끊김 및 웹을 닫을 경우.
            map.put("roomStatus", "STOPPED");
            map.put("status", "LEFT");
            map.put("roomNo", roomNo);
            restTemplate.postForObject(apiBaseUrl + "/api/twenty/room/disconnect/teacher",
                map, Void.class);
            map.clear();
            List<RoomUser> list = Arrays.asList(
                restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
                    RoomUser[].class)
            );
            map.put("list", list);
            map.put("roomStatus", "STOPPED");
        }
        template.convertAndSend("/topic/TeacherDisconnect/" + roomNo, map);
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

        List<RoomUser> list = Arrays.asList(
            restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo + "/players",
                RoomUser[].class)
        );
        map.put("list", list);
        template.convertAndSend("/topic/participants/" + roomNo, map);
    }

    /**
     * 손들기 버튼을 누르면이 메소드로 이동
     * 1. 게임방을 AWAITING_INPUT 상태로 변경
     * 2. 전달 받은 userNo가 빠른 처리를 한 것인지 확인.
     * 3. true면 map 객체에 userNo와 변경된 게임방의 상태를 담아고 40초 제한시간 부여
     * 4. 바로 브로드 캐스팅
     * @param roomNo
     * @param userNo
     */
    public void raiseHand(int roomNo, int userNo, String userNickName) {
        if (redisLockManager.trySetQuestioner(roomNo, userNo)) {
            // 상태 변경 요청
            restTemplate.postForEntity(
                apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=AWAITING_INPUT",
                null, Void.class);

            // 손들기 버튼을 누르면 서버에서 40초 제한시간이 설정됨.
            ScheduledFuture<?> scheduledFuture =
                taskScheduler.schedule(() -> turnTimeout(roomNo), Instant.now().plusSeconds(40));
            scheduledTasks.put(roomNo, scheduledFuture);

            // 브로드캐스팅
            Map<String, Object> map = new HashMap<>();
            map.put("roomStatus", "AWAITING_INPUT");
            map.put("userNo", userNo);
            map.put("name", userNickName);
            map.put("time", 40); // 프론트에서 시간이 지나는 것을 확인하기 위해서 값을 브로드 캐스팅하는 것.
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
        redisLockManager.releaseQuestionerLock(roomNo);

        TwentyRoom room = restTemplate.getForObject(apiBaseUrl + "/api/twenty/room/" + roomNo,
            TwentyRoom.class);

        if ("AWAITING_INPUT".equals(room.getStatus())) {
            restTemplate.postForEntity(
                apiBaseUrl + "/api/twenty/room/" + roomNo + "/status?status=IN_PROGRESS",
                null, Void.class);

            Map<String, Object> map = new HashMap<>();
            map.put("roomStatus", "IN_PROGRESS");
            template.convertAndSend("/topic/turnTimeout/" + roomNo, map);
        }
        scheduledTasks.remove(roomNo);
    }

    /**
     * 학생이 40초 안에 질문 또는 답변을 제출했을 경우
     * 1. 먼저 타이머를 취소
     * 2. Redis에 저장된 1등 학생을 삭제한다.
     * 3. 게임방의 상태를 "AWAITING_RESPONSE"로 변경
     * 4. 학생이 제출한 메세지가 질문인지 답변인지 확인 -> log 테이블에 할당
     * 5. 메세지와, 게임방의 상태 값을 map 객체에 담아 브로드 캐스팅
     */
    public void submitAnswerOrQuestion(int roomNo, int userNo) {
        Map<String, Object> map = new HashMap<>();
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(roomNo);
        if (scheduledTask != null) {
            //아까 그 40초 뒤에 일어나는 행위들을 전부 취소 시켜주는 메소드.
            scheduledTask.cancel(false);
            scheduledTasks.remove(roomNo);
        }
        redisLockManager.releaseQuestionerLock(roomNo);
        //... 기타 로직 작성
    }

}
