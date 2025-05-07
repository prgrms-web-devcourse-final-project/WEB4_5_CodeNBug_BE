package org.codenbug.user.sns.util;

import org.codenbug.user.sns.constant.SocialLoginType;

public interface SocialOauth { //소셜 로그인 타입별로 공통적으로 사용될 interface
    /**
     * 각 Social Login 페이지로 Redirect 처리할 URL Build
     * 사용자로부터 로그인 요청을 받아 Social Login Server 인증용 code 요청
     */
    String getOauthRedirectURL();

    /**
     * 각 Social Login 페이지로 Redirect 처리할 URL Build (커스텀 리다이렉트 URL 사용)
     * @param redirectUrl 커스텀 리다이렉트 URL
     * @return Social Login 페이지 URL
     */
    String getOauthRedirectURL(String redirectUrl);

    /**
     * API Server로부터 받은 code를 활용하여 사용자 인증 정보 요청
     * @param code API Server 에서 받아온 code
     * @return API 서버로 부터 응답받은 Json 형태의 결과를 string으로 반환
     */
    String requestAccessToken(String code);

    /**
     * API Server로부터 받은 code를 활용하여 사용자 인증 정보 요청 (커스텀 리다이렉트 URL 사용)
     * @param code API Server 에서 받아온 code
     * @param redirectUrl 커스텀 리다이렉트 URL
     * @return API 서버로 부터 응답받은 Json 형태의 결과를 string으로 반환
     */
    String requestAccessToken(String code, String redirectUrl);

    default SocialLoginType type() {
        if (this instanceof GoogleOauth) {
            return SocialLoginType.GOOGLE;
        } else if (this instanceof KakaoOauth) {
            return SocialLoginType.KAKAO;
        } else {
            return null;
        }
    }
}