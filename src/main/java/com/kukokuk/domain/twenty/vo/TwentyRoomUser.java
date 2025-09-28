package com.kukokuk.domain.twenty.vo;

import com.kukokuk.domain.user.vo.User;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TwentyRoomUser {

    private int userNo;
    private int roomNo;
    private String status;
    private Date createdDate;
    private Date updatedDate;

    private User user;
}
