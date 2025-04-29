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

    public RedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Refresh Token을 Redis에 저장
     *
     * @param email 사용자 이메일
     * @param refreshToken Refresh Token
     * @param expirationTime 만료 시간 (밀리초)
     */
    public void saveRefreshToken(String email, String refreshToken, long expirationTime) {
        redisTemplate.opsForValue().set(email, refreshToken, expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Redis에서 Refresh Token을 조회
     *
     * @param email 사용자 이메일
     * @return Refresh Token
     */
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    /**
     * Redis에서 Refresh Token을 삭제
     *
     * @param email 사용자 이메일
     */
    public void deleteRefreshToken(String email) {
        redisTemplate.delete(email);
    }

    /**
     * Refresh Token이 Redis에 존재하는지 확인
     *
     * @param email 사용자 이메일
     * @return 존재 여부
     */
    public boolean existsRefreshToken(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(email));
    }
} 