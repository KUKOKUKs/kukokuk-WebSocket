package com.kukokuk.common.util;

/**
 * 파일 관련 경로를 생성해주는 유틸 클래스
 */
public class FilePathUtil {

    // 경로 상수
    public static final String PROFILE_DIR = "/images/profile/";
    public static final String BASE_PROFILE = "/images/basic_profile_img.jpg";

    /**
     * 프로필 이미지 전체 URL 생성
     * @param userNo 사용자 번호
     * @param filename 파일명
     * @return 완전한 URL 경로 (/images/profile/{userNo}/{filename})
     */
    public static String getProfileImagePath(int userNo, String filename) {
        if (filename == null || filename.isBlank()) return BASE_PROFILE;
        return PROFILE_DIR + userNo + "/" + filename;
    }

}
