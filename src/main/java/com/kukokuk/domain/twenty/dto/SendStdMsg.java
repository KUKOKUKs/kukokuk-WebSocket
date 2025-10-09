package com.kukokuk.domain.twenty.dto;

import lombok.Getter;
import lombok.Setter;

// 학생이 보낸 메세지를 담는 DTO 객체
@Getter
@Setter
public class SendStdMsg {

    private int logNo;
    private int userNo;
    private int roomNo;
    private String content;
    private String type; // 질문인 경우 Q, 정답인 경우 A
    private Integer  cnt;
    private String isSuccess; // 정답에 대한 O,X
    private String answer;    // 질문에 대한 O,X
    String nickName;          //유저 닉네임
}
