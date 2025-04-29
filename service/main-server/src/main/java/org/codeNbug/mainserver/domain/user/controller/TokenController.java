package org.codeNbug.mainserver.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.user.dto.request.RefreshTokenRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Refresh Token을 이용해 새로운 Access Token을 발급
     *
     * @param request Refresh Token 요청 정보
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String newAccessToken = tokenService.refreshAccessToken(request.getEmail(), request.getRefreshToken());
        return ResponseEntity.ok(LoginResponse.of(newAccessToken, request.getRefreshToken()));
    }
} 