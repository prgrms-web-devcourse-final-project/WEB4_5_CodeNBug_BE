package org.codeNbug.mainserver.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("accessToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/api/v1/auth/refresh");
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