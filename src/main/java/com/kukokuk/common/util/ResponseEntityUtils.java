package com.kukokuk.common.util;

import com.kukokuk.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * HTTP 응답(ResponseEntity)을 조작하는 역할 HTTP 상태 코드 설정 ResponseEntity를 이용한 HTTP 응답 포장 내부에는 항상
 * ApiResponse를 넣지만, 포장은 HTTP 컨트롤러의 책임
 */
public class ResponseEntityUtils {

    // 성공 응답- status:200, message:메세지, data:null
    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ResponseEntity
            .status(200)
            .body(ApiResponse.success(message));
    }

    // 성공 응답 - status:200, message:"성공", data:데이터
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity
            .status(200)
            .body(ApiResponse.success(data));
    }

    // 성공 응답 - status:200, message:메세지, data:데이터
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity
            .status(200)
            .body(ApiResponse.success(message, data));
    }

    // 실패 응답 - status:응답코드, message:메세지, data:null
    public static ResponseEntity<ApiResponse<Void>> fail(int status, String message) {
        return ResponseEntity
            .status(status)
            .body(ApiResponse.fail(status, message));
    }

}
