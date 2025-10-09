package com.kukokuk.domain.twenty.dto;

import com.kukokuk.domain.user.vo.User;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WsUser implements Serializable {

    private int userNo;
    private String nickName;
    private List<String> roleNames;

    public WsUser(User user) {
        this.userNo = user.getUserNo();
        this.nickName = user.getNickname();
        this.roleNames = user.getRoleNames();
    }
}
