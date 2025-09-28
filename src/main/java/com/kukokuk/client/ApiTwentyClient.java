package com.kukokuk.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ApiTwentyClient {
    private final RestTemplate restTemplate;

    @Value("${api.server.base-url}")
    private String apiBaseUrl;

    public void updateRoomStatus(int roomNo, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomNo", roomNo);
        body.put("roomStatus", status);
        restTemplate.postForEntity(apiBaseUrl + "/api/twenty/updateRoomStatus", body, Void.class);
    }

    public List<Map<String,Object>> getPlayers(int roomNo) {
        return Arrays.asList(
            restTemplate.getForObject(apiBaseUrl + "/api/twenty/players/" + roomNo, Map[].class)
        );
    }
}

