package org.codeNbug.mainserver.global.Redis.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 사용한 토큰 관리를 위한 Repository
 */
@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final StringRedisTemplate template;

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
     * Redis에서 Refresh Token으로 식별자(이메일 또는 소셜ID)를 조회
     *
     * @param refreshToken Refresh Token
     * @return 사용자 식별자(이메일 또는 소셜ID:provider)
     */
    public String getIdentifierByRefreshToken(String refreshToken) {
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

    public String getData(String key) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        return valueOperations.get(key);
    }

    public boolean existData(String key) {
        return Boolean.TRUE.equals(template.hasKey(key));
    }

    public void setDataExpire(String key, String value, long duration) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }

    public void deleteData(String key) {
        template.delete(key);
    }
} 