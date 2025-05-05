package org.codeNbug.mainserver.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${cookie.secure}")
    private boolean isSecure;

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일
    private static final String COOKIE_PATH = "/";

    /**
     * 액세스 토큰 쿠키 설정
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
        cookie.setMaxAge((int) (accessTokenExpiration / 1000)); // milliseconds to seconds
        cookie.setPath(COOKIE_PATH);
        // cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        
        // 개발 환경 확인 (localhost에서는 secure=false로 설정)
        boolean isDev = "dev".equals(activeProfile);
        cookie.setSecure(isDev ? false : isSecure);
        
        // SameSite 속성을 Lax로 변경 (크로스 사이트 요청에 더 관대)
        cookie.setAttribute("SameSite", "Lax");
        
        response.addCookie(cookie);
    }

    /**
     * 리프레시 토큰 쿠키 설정
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, token);
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000)); // milliseconds to seconds
        cookie.setPath(COOKIE_PATH);
        // cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        
        // 개발 환경 확인 (localhost에서는 secure=false로 설정)
        boolean isDev = "dev".equals(activeProfile);
        cookie.setSecure(isDev ? false : isSecure);
        
        // SameSite 속성을 Lax로 변경
        cookie.setAttribute("SameSite", "Lax");
        
        response.addCookie(cookie);
    }

    /**
     * 액세스 토큰 쿠키 삭제
     */
    public void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setPath(COOKIE_PATH);
        // cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        
        // 개발 환경 확인
        boolean isDev = "dev".equals(activeProfile);
        cookie.setSecure(isDev ? false : isSecure);
        
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * 리프레시 토큰 쿠키 삭제
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setPath(COOKIE_PATH);
        // cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        
        // 개발 환경 확인
        boolean isDev = "dev".equals(activeProfile);
        cookie.setSecure(isDev ? false : isSecure);
        
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * 요청에서 액세스 토큰 추출
     */
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 요청에서 리프레시 토큰 추출
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
} 