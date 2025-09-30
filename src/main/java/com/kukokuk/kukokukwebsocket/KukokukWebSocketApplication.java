package com.kukokuk.kukokukwebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages ="com.kukokuk")
public class KukokukWebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(KukokukWebSocketApplication.class, args);
    }

}
