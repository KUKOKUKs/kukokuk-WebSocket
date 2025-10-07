package com.kukokuk.domain.twenty.dto;

import lombok.Getter;
import lombok.Setter;
// 학생이 보낸 메세지를 담는 DTO 객체
@Getter
@Setter
public class SendStdMsg {
    private int userNo;
    private int roomNo;
    private String msg;
    private String type; // 질문인 경우 Q, 정답인 경우 A
}
