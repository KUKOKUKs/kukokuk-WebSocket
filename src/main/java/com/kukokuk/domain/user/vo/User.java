package com.kukokuk.domain.user.vo;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int userNo;                         // 사용자 번호
    private String username;                    // 이메일
    private String password;                    // 비밀번호
    private String name;                        // 이름
    private String nickname;                    // 닉네임
    private Date birthDate;                     // 생년월일
    private String gender;                      // 성별 ENUM("M", "F")
    private String profileFilename;             // 프로필 이미지 파일명
    private String authProvider;                // 제 3자 로그인 시 소셜 종류
    private Integer level;                      // 레벨
    private Integer experiencePoints;           // 누적 경험치
    private Integer studyDifficulty;            // 학습 단계
    private String currentSchool;               // 학교 ENUM("초등","중등")
    private Integer currentGrade;               // 학년
    private Integer hintCount;                  // 힌트 개수
    private String isDeleted;                   // ENUM("N", "Y")
    private Date createdDate;
    private Date updatedDate;

    private Integer maxExp;                     // 레벨업에 필요한 누적 경험치
    private List<String> roleNames;             // 사용자 권한 정보 목록
    private Integer groupNo;                    // 사용자가 속한 그룹 번호(그룹에 속하지 않았으면 Null)

    public String getProfileFileUrl() {
        if (profileFilename == null || profileFilename.isBlank()) {
            return null;
        }
        return "/images/profile/" + userNo + "/" + profileFilename;
    }

}
