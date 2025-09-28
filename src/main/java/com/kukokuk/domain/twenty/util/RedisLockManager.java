package com.kukokuk.domain.twenty.util;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLockManager {

    // * Redis라는 것은 서버에 저장할 수있는 DB 같은 것이다.
    private final RedisTemplate<String, String> redisTemplate; // Redis를 사용하기 위한 변수

    /**
     * roomNo를 가진 게임방에 가장 빨리 누른 userNo가 맞는지 검사하는 메소드 1.해당 방의 번호를 가진, 1등 userNo를 담을 문자열 변수 생성
     * 2.opsValue()를 사용하여 key-value 형태로 데이터를 담을 수 있도록 설정 3.setIfAnsent()을 사용하여 lockKey이 값의 value가
     * 없다면 추가해서 true 반환/ 있다면 추가하지 말고 false 반환 - Duration.ofSeconds(45) 혹여나 서버가 다운된다면, 45초 뒤에 다시 초기화
     * 된다.
     *
     * @param roomNo
     * @param userNo
     * @return
     */
    public boolean trySetQuestioner(int roomNo, int userNo) { // roomNo = 1 , userNo: 10
        String lockKey = "lock:questioner:" + roomNo;           // lockKey = lock:questioner : 1
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, String.valueOf(userNo), Duration.ofSeconds(45));
        // lock:questioner : 1 - 10 이 추가됨. -> true 반환
        // 이 상태에서 다른 유저가 요청하면, false가 반환.
        return Boolean.TRUE.equals(success);
    }

    /**
     * 한 턴이 끝났을 때, 다시 초기화하는 메소드
     *
     * @param roomNo
     */
    public void releaseQuestionerLock(int roomNo) {
        String lockKey = "lock:questioner:" + roomNo;
        redisTemplate.delete(lockKey);
    }
}
