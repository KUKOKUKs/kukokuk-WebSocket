package com.kukokuk.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응답 데이터 구조를 정의하는 역할
 * 응답 JSON의 구조를 통일 (success, status, message, data)
 * 응답 내용을 Java 객체로 표현
 * 상태코드는 기본값으로 설정 / ResponseEntityUtils에서 상태코드  설정 담당
 */
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private int status;
    private String message;
    private T data;

    public ApiResponse(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // 성공 응답- status:200, message:메세지, data:null
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<Void>(true, 200, message, null);
    }

    // 성공 응답 - status:200, message:"성공", data:데이터
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, 200, "성공", data);
    }

    // 성공 응답 - status:200, message:메세지, data:데이터
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(true, 200, message, data);
    }

    // 실패 응답 - status:응답코드, message:메세지, data:null
    public static ApiResponse<Void> fail(int status, String message) {
        return new ApiResponse<Void>(false, status, message, null);
    }

}
