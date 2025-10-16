package com.kukokuk.domain.twenty.vo;

import com.kukokuk.domain.user.vo.User;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TwentyRoom {

    private int roomNo;
    private String correctAnswer;
    private String isSuccess;
    private String status;
    private Date createdDate;
    private Date updatedDate;
    private int groupNo;
    private int winnerNo;
    private int tryCnt;
    private String title;

    private User user;
}
