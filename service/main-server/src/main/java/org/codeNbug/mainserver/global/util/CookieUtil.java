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
        cookie.setPath("/");
        cookie.setMaxAge((int) (accessTokenExpiration / 1000)); // 밀리초를 초로 변환
        
        // 기본 쿠키 속성 설정
        cookie.setHttpOnly(true);
        
        // 프로덕션 환경에서 보안 설정 추가
        if (!"dev".equals(activeProfile)) {
            cookie.setSecure(true);
            // SameSite=None 설정을 위한 헤더 추가
            response.addHeader("Set-Cookie", 
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None", 
                cookie.getName(), 
                cookie.getValue(), 
                cookie.getMaxAge()));
        } else {
            response.addCookie(cookie);
        }
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        // 기존 refresh token 쿠키를 삭제
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);

        // 새로운 refresh token 쿠키를 설정
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        
        // 기본 쿠키 속성 설정
        cookie.setHttpOnly(true);
        
        // 프로덕션 환경에서 보안 설정 추가
        if (!"dev".equals(activeProfile)) {
            cookie.setSecure(true);
            // SameSite=None 설정을 위한 헤더 추가
            response.addHeader("Set-Cookie", 
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None", 
                cookie.getName(), 
                cookie.getValue(), 
                cookie.getMaxAge()));
        } else {
            response.addCookie(cookie);
        }
    }
} 