package org.codeNbug.mainserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 시도 횟수 관리 서비스
 * Redis를 사용하여 로그인 시도 횟수를 관리합니다.
 * 계정 잠금은 Redis의 TTL을 활용해 자동 관리됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    
    // 최대 로그인 시도 횟수 (5회)
    private static final int MAX_ATTEMPTS = 5;
    
    // 계정 잠금 지속 시간 (분)
    private static final int LOCK_DURATION_MINUTES = 3;
    
    // 로그인 시도 횟수 만료 시간 (분) - 일정 시간 후 시도 횟수 리셋
    private static final int ATTEMPT_EXPIRY_MINUTES = 30;
    
    // Redis 계정 잠금 키 접두사
    private static final String ACCOUNT_LOCK_PREFIX = "accountLock:";
    
    // Redis 로그인 시도 횟수 키 접두사
    private static final String LOGIN_ATTEMPT_PREFIX = "loginAttempt:";
    
    /**
     * 데이터베이스에 계정 잠금 상태 동기화 (백업용)
     */
    private void updateAccountLockedInDb(String email, boolean locked) {
        try {
            String sql = "UPDATE users SET account_locked = ? WHERE email = ?";
            int updatedRows = jdbcTemplate.update(sql, locked, email);
            log.debug(">> DB 계정 잠금 상태 동기화: email={}, locked={}, 성공={}", 
                    email, locked, updatedRows > 0);
        } catch (Exception e) {
            log.warn(">> DB 계정 잠금 상태 동기화 실패: {}", e.getMessage());
        }
    }
    
    /**
     * Redis에 계정 잠금 정보 설정
     * 
     * @param email 사용자 이메일
     */
    private void lockAccountInRedis(String email) {
        String key = ACCOUNT_LOCK_PREFIX + email;
        redisTemplate.opsForValue().set(key, 
                                      LocalDateTime.now().toString(), 
                                      LOCK_DURATION_MINUTES, 
                                      TimeUnit.MINUTES);
        log.info(">> Redis에 계정 잠금 설정 완료: email={}, 잠금시간={}분", email, LOCK_DURATION_MINUTES);
    }
    
    /**
     * Redis에서 계정 잠금 해제
     * 
     * @param email 사용자 이메일
     */
    private void unlockAccountInRedis(String email) {
        String key = ACCOUNT_LOCK_PREFIX + email;
        Boolean deleted = redisTemplate.delete(key);
        log.info(">> Redis에서 계정 잠금 해제: email={}, 성공={}", 
                email, deleted != null && deleted);
        
        // 데이터베이스 동기화 (백업)
        updateAccountLockedInDb(email, false);
    }
    
    /**
     * 사용자의 최대 로그인 시도 횟수 조회
     *
     * @param userId 사용자 ID
     * @return 사용자의 최대 로그인 시도 횟수, 조회 실패 시 null
     */
    public Integer getUserMaxLoginAttempts(Long userId) {
        try {
            String sql = "SELECT max_login_attempts FROM users WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            log.warn(">> 최대 로그인 시도 횟수 조회 중 오류 발생: {}", e.getMessage());
            return MAX_ATTEMPTS; // 기본값 사용
        }
    }
    
    /**
     * 계정 잠금 상태 확인
     * Redis에서 계정 잠금 키 존재 여부 확인
     *
     * @param email 사용자 이메일
     * @return true: 계정이 잠김, false: 계정이 잠기지 않음
     */
    public boolean isAccountLocked(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        String key = ACCOUNT_LOCK_PREFIX + email;
        Boolean hasKey = redisTemplate.hasKey(key);
        boolean isLocked = Boolean.TRUE.equals(hasKey);
        
        if (isLocked) {
            log.info(">> 계정 잠금 상태 확인: email={}, 잠금상태=true", email);
            
            // TTL 확인 (디버깅용)
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                log.debug(">> 계정 잠금 남은 시간: email={}, 남은시간={}초", email, ttl);
            }
        }
        
        return isLocked;
    }
    
    /**
     * 모든 사용자의 로그인 시도 횟수 초기화
     *
     * @return 초기화된 사용자 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int resetAllLoginAttempts() {
        try {
            log.info(">> 모든 사용자 로그인 시도 횟수 초기화 시작");
            
            // Redis에서 모든 로그인 시도 횟수 키 삭제
            Set<String> attemptKeys = redisTemplate.keys(LOGIN_ATTEMPT_PREFIX + "*");
            if (attemptKeys != null && !attemptKeys.isEmpty()) {
                redisTemplate.delete(attemptKeys);
                log.info(">> Redis에서 모든 로그인 시도 키 삭제 완료: 키 수={}", attemptKeys.size());
            }
            
            // Redis에서 모든 계정 잠금 키 삭제
            Set<String> lockKeys = redisTemplate.keys(ACCOUNT_LOCK_PREFIX + "*");
            if (lockKeys != null && !lockKeys.isEmpty()) {
                redisTemplate.delete(lockKeys);
                log.info(">> Redis에서 모든 계정 잠금 키 삭제 완료: 키 수={}", lockKeys.size());
            }
            
            // 단순 SQL 쿼리로 모든 사용자 초기화 (백업용)
            String sql = "UPDATE users SET login_attempt_count = 0, account_locked = false";
            int updatedRows = jdbcTemplate.update(sql);
            log.info(">> 데이터베이스 로그인 시도 횟수 초기화 완료: 초기화된 계정 수={}", updatedRows);
            
            return (attemptKeys != null ? attemptKeys.size() : 0) + (lockKeys != null ? lockKeys.size() : 0);
        } catch (Exception e) {
            log.error(">> 모든 사용자 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Redis 템플릿 반환 (관리자 기능에서 사용)
     *
     * @return RedisTemplate 객체
     */
    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 사용자 이메일로 로그인 시도 횟수 조회
     *
     * @param email 사용자 이메일
     * @return 현재 로그인 시도 횟수
     */
    public Integer getAttemptCountByEmail(String email) {
        try {
            if (email == null || email.isEmpty()) {
                return 0;
            }
            
            // Redis에서 시도 횟수 조회
            String attemptKey = LOGIN_ATTEMPT_PREFIX + email;
            String countStr = redisTemplate.opsForValue().get(attemptKey);
            
            if (countStr != null) {
                try {
                    return Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    log.warn(">> Redis 로그인 시도 횟수 변환 오류: {}", e.getMessage());
                }
            }
            
            // Redis에 없으면 0 반환 (새로운 시도로 간주)
            return 0;
        } catch (Exception e) {
            log.warn(">> 로그인 시도 횟수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 사용자 이메일로 로그인 시도 횟수 초기화
     *
     * @param email 사용자 이메일
     * @return 초기화 성공 여부
     */
    public boolean resetAttemptByEmail(String email) {
        try {
            if (email == null || email.isEmpty()) {
                return false;
            }
            
            // Redis에서 로그인 시도 횟수 키 삭제
            String attemptKey = LOGIN_ATTEMPT_PREFIX + email;
            Boolean attemptDeleted = redisTemplate.delete(attemptKey);
            
            // Redis에서 계정 잠금 키 삭제
            unlockAccountInRedis(email);
            
            log.info(">> 이메일 기반 로그인 시도 횟수 초기화: email={}, 성공={}", 
                    email, attemptDeleted != null && attemptDeleted);
            
            return true;
        } catch (Exception e) {
            log.warn(">> 이메일 기반 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 사용자 이메일로 로그인 시도 횟수 증가
     *
     * @param email 사용자 이메일
     * @param maxAttempts 최대 시도 횟수 (기본값: 5)
     * @return true: 계정이 잠겼음, false: 계정이 잠기지 않음
     */
    public boolean incrementAttemptByEmail(String email, Integer maxAttempts) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        try {
            log.info(">> 이메일 기반 로그인 시도 횟수 증가 시작: email={}", email);
            
            // Redis 키 생성
            String attemptKey = LOGIN_ATTEMPT_PREFIX + email;
            
            // 현재 시도 횟수 가져오기 및 증가
            Long currentAttempts = redisTemplate.opsForValue().increment(attemptKey);
            
            // 키가 없었다면 만료 시간 설정 (30분 후 자동 삭제)
            if (currentAttempts != null && currentAttempts == 1) {
                redisTemplate.expire(attemptKey, ATTEMPT_EXPIRY_MINUTES, TimeUnit.MINUTES);
            }
            
            log.info(">> Redis 이메일 기반 로그인 시도 횟수 증가: email={}, 현재 시도 횟수={}", 
                    email, currentAttempts);
            
            // 최대 시도 횟수 처리
            int maxAllowedAttempts = maxAttempts != null ? maxAttempts : MAX_ATTEMPTS;
            
            // 최대 시도 횟수 초과 시 계정 잠금
            if (currentAttempts != null && currentAttempts >= maxAllowedAttempts) {
                // Redis에 계정 잠금 설정
                lockAccountInRedis(email);
                log.info(">> 이메일 기반 계정이 잠겼습니다: email={}, 시도 횟수={}/{}", 
                        email, currentAttempts, maxAllowedAttempts);
                
                // 데이터베이스 동기화
                updateAccountLockedInDb(email, true);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error(">> 이메일 기반 로그인 시도 횟수 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 이메일로 계정 잠금 시간 조회
     * 
     * @param email 사용자 이메일
     * @return 남은 잠금 시간(분), 잠금 시간이 지났거나 잠기지 않은 경우 0
     */
    public long getRemainingLockTimeByEmail(String email) {
        try {
            if (email == null || email.isEmpty()) {
                return 0;
            }
            
            String key = ACCOUNT_LOCK_PREFIX + email;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.warn(">> 이메일 기반 계정 잠금 시간 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
} 