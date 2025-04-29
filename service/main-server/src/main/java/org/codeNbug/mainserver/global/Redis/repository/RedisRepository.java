package org.codeNbug.mainserver.global.Redis.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Redis를 사용한 토큰 관리를 위한 Repository
 */
@Repository
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public RedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Refresh Token을 Redis에 저장
     *
     * @param refreshToken Refresh Token
     * @param email 사용자 이메일
     * @param expirationTime 만료 시간 (밀리초)
     */
    public void saveRefreshToken(String refreshToken, String email, long expirationTime) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, email, expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Redis에서 Refresh Token으로 이메일을 조회
     *
     * @param refreshToken Refresh Token
     * @return 사용자 이메일
     */
    public String getEmailByRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에서 Refresh Token을 삭제
     *
     * @param refreshToken Refresh Token
     */
    public void deleteRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        redisTemplate.delete(key);
    }

    /**
     * Refresh Token이 Redis에 존재하는지 확인
     *
     * @param refreshToken Refresh Token
     * @return 존재 여부
     */
    public boolean existsRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
} 