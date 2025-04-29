package org.codeNbug.mainserver.domain.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.codeNbug.mainserver.global.util.CookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토큰 관련 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final CookieUtil cookieUtil;

    /**
     * Refresh Token을 이용해 새로운 Access Token을 발급
     *
     * @param request HTTP 요청 객체 (쿠키에서 refresh token 추출용)
     * @param response HTTP 응답 객체 (새로운 access token 쿠키 설정용)
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 쿠키에서 refresh token 추출
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.builder()
                            .tokenType("Bearer")
                            .accessToken(null)
                            .refreshToken(null)
                            .build());
        }

        // 토큰 재발급 시도
        TokenService.TokenInfo tokenInfo = tokenService.refreshTokens(refreshToken);
        
        // 새로운 access token을 쿠키에 설정
        cookieUtil.setAccessTokenCookie(response, tokenInfo.getAccessToken());
        
        // refresh token 쿠키 재설정
        cookieUtil.setRefreshTokenCookie(response, tokenInfo.getRefreshToken());
        
        // refresh token은 이미 쿠키에 있으므로 응답 본문에서는 제외
        return ResponseEntity.ok(LoginResponse.ofTokenTypeOnly());
    }
} 