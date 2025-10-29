package com.kukokuk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    /**
     * 스프링이 제공하는 스케줄링 전용 스레드 풀
     * 일반적인 스케줄 작업이나 예약 작업을 하기 위해 사용. (ThreadPoolTaskScheduler)
     * - 손들기 버튼에 대해서, 40초의 제한시간을 두기 때문에, 40초가 지나면 자동으로 턴 종료를 해야하기 때문 .
     * @return
     */
    @Bean
    @Primary
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);                   //최대 5개의 타이머 작업을 할 수 있음.
        scheduler.setThreadNamePrefix("ws-sched-"); //스케줄러가 만드는 스레드 이름. -> 나중에 로그 찍을 때, WebSocket 스케줄러인 것을 확인 할 수 있음.
        return scheduler;
    }
}

