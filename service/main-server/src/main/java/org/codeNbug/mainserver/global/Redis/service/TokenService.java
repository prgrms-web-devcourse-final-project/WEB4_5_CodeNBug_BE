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
        redisRepository.saveRefreshToken(email, refreshToken, jwtConfig.getRefreshTokenExpiration());

        return TokenInfo.of(accessToken, refreshToken);
    }

    /**
     * Refresh Token을 이용해 새로운 Access Token을 발급
     *
     * @param email 사용자 이메일
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token
     * @throws InvalidTokenException Refresh Token이 유효하지 않은 경우
     */
    @Transactional
    public String refreshAccessToken(String email, String refreshToken) {
        // Redis에서 Refresh Token 조회
        String storedRefreshToken = redisRepository.getRefreshToken(email);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 새로운 Access Token 발급
        return jwtConfig.generateAccessToken(email);
    }

    /**
     * 로그아웃 시 토큰을 무효화
     *
     * @param email 사용자 이메일
     */
    @Transactional
    public void invalidateTokens(String email) {
        redisRepository.deleteRefreshToken(email);
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