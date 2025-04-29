package org.codeNbug.mainserver.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("accessToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        setCommonCookieAttributes(cookie);
        cookie.setPath("/auth/refresh");
        response.addCookie(cookie);
    }

    private void setCommonCookieAttributes(Cookie cookie) {
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
    }
} 