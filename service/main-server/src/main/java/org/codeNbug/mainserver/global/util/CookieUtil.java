package org.codeNbug.mainserver.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("accessToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) (accessTokenExpiration / 1000)); // 밀리초를 초로 변환
        response.addCookie(cookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        // 기존 refresh token 쿠키를 삭제
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);

        // 새로운 refresh token 쿠키를 설정
        Cookie cookie = new Cookie("refreshToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(cookie);
    }

    private void setCommonCookieAttributes(Cookie cookie) {
        cookie.setHttpOnly(true);
        
        // 개발 환경에서는 Secure와 SameSite 설정을 하지 않음
        if (!"dev".equals(activeProfile)) {
            cookie.setSecure(true);
            // SameSite 속성은 response header에 직접 설정해야 함
            // cookie.setAttribute("SameSite", "None");
        }
    }
} 