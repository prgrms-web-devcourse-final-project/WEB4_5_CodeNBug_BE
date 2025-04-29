package org.codeNbug.mainserver.global.Redis.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.global.exception.security.InvalidTokenException;
import org.codeNbug.mainserver.global.Redis.repository.RedisRepository;
import org.codeNbug.mainserver.global.util.JwtConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 관리를 위한 서비스
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;
    private final RedisRepository redisRepository;

    /**
     * Access Token과 Refresh Token을 생성하고, Refresh Token을 Redis에 저장
     *
     * @param email 사용자 이메일
     * @return 생성된 토큰 정보
     */
    @Transactional
    public TokenInfo generateTokens(String email) {
        String accessToken = jwtConfig.generateAccessToken(email);
        String refreshToken = jwtConfig.generateRefreshToken(email);

        // Refresh Token을 Redis에 저장
        redisRepository.saveRefreshToken(refreshToken, email, jwtConfig.getRefreshTokenExpiration());

        return TokenInfo.of(accessToken, refreshToken);
    }

    /**
     * Refresh Token을 검증하고 새로운 토큰을 발급
     *
     * @param refreshToken Refresh Token
     * @return 토큰 정보 (Access Token과 필요한 경우 새 Refresh Token)
     * @throws InvalidTokenException Refresh Token이 유효하지 않은 경우
     */
    @Transactional
    public TokenInfo refreshTokens(String refreshToken) {
        // Redis에서 Refresh Token으로 이메일 조회 및 JWT 유효성 검증
        String email = redisRepository.getEmailByRefreshToken(refreshToken);
        boolean isJwtValid = jwtConfig.validateToken(refreshToken);

        // Redis에 토큰이 없거나 JWT가 유효하지 않은 경우
        if (email == null || !isJwtValid) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // Access Token만 새로 발급
        String newAccessToken = jwtConfig.generateAccessToken(email);
        
        return TokenInfo.of(newAccessToken, refreshToken);
    }

    /**
     * 로그아웃 시 토큰을 무효화
     *
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void invalidateTokens(String refreshToken) {
        redisRepository.deleteRefreshToken(refreshToken);
    }

    /**
     * 토큰 정보를 담는 내부 클래스
     */
    @RequiredArgsConstructor
    @Getter
    public static class TokenInfo {
        private final String accessToken;
        private final String refreshToken;

        public static TokenInfo of(String accessToken, String refreshToken) {
            return new TokenInfo(accessToken, refreshToken);
        }
    }
} 