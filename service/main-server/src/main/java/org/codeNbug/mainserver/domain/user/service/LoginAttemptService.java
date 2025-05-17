package org.codeNbug.mainserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
 * JdbcTemplate을 사용해 데이터베이스에 직접 접근하여 로그인 시도 횟수를 관리합니다.
 * 계정 잠금은 Redis를 통해 TTL로 자동 관리됩니다.
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
    
    // Redis 계정 잠금 키 접두사
    private static final String ACCOUNT_LOCK_PREFIX = "accountLock:";

    /**
     * 로그인 시도 횟수 증가 및 계정 잠금 상태 업데이트
     * 항상 새로운 트랜잭션에서 실행되어 DB에 즉시 반영됩니다.
     *
     * @param userId 사용자 ID
     * @return true: 계정이 잠겼음, false: 계정이 잠기지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean incrementAttempt(Long userId) {
        if (userId == null) {
            log.warn(">> 로그인 시도 횟수 증가 실패: 사용자 ID가 null입니다.");
            return false;
        }
        
        try {
            log.info(">> 로그인 시도 횟수 증가 시작: userId={}", userId);
            
            // 현재 시간
            LocalDateTime now = LocalDateTime.now();
            
            // 단순 SQL 쿼리로 카운트 증가 (MySQL 구문)
            String sql = "UPDATE users SET login_attempt_count = IFNULL(login_attempt_count, 0) + 1, " +
                         "last_login_at = ? WHERE user_id = ?";
            int updatedRows = jdbcTemplate.update(sql, now, userId);
            
            if (updatedRows != 1) {
                log.warn(">> 로그인 시도 횟수 업데이트 실패: 업데이트된 행 수={}", updatedRows);
                return false;
            }
            
            // 현재 시도 횟수 조회
            Integer currentAttempts = getCurrentAttemptCount(userId);
            log.info(">> 로그인 시도 횟수 업데이트 성공: userId={}, 현재 시도 횟수={}", userId, currentAttempts);
            
            // 사용자의 최대 시도 횟수 조회
            Integer maxAttempts = getUserMaxLoginAttempts(userId);
            if (maxAttempts == null) {
                maxAttempts = MAX_ATTEMPTS; // 기본값 사용
            }
            
            // 최대 시도 횟수 초과 시 계정 잠금
            if (currentAttempts != null && currentAttempts >= maxAttempts) {
                // 사용자 이메일 조회
                String email = getUserEmail(userId);
                if (email != null) {
                    // Redis에 계정 잠금 설정
                    lockAccountInRedis(email);
                    log.info(">> 계정이 잠겼습니다: userId={}, email={}, 시도 횟수={}/{}", 
                            userId, email, currentAttempts, maxAttempts);
                    return true;
                } else {
                    log.warn(">> 계정 잠금 실패: 사용자 이메일을 찾을 수 없음, userId={}", userId);
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error(">> 로그인 시도 횟수 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return false;
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
     * 사용자 이메일 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 이메일
     */
    private String getUserEmail(Long userId) {
        try {
            String sql = "SELECT email FROM users WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            log.warn(">> 사용자 이메일 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 로그인 시도 횟수 초기화 및 계정 잠금 해제
     * 항상 새로운 트랜잭션에서 실행되어 DB에 즉시 반영됩니다.
     *
     * @param userId 사용자 ID
     * @return 업데이트 성공 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean resetAttempt(Long userId) {
        if (userId == null) {
            log.warn(">> 로그인 시도 횟수 초기화 실패: 사용자 ID가 null입니다.");
            return false;
        }
        
        try {
            log.info(">> 로그인 시도 횟수 초기화 시작: userId={}", userId);
            
            // 현재 시간
            LocalDateTime now = LocalDateTime.now();
            
            // 단순 SQL 쿼리로 초기화 (account_locked는 Redis로 관리하므로 업데이트 X)
            String sql = "UPDATE users SET login_attempt_count = 0, last_login_at = ? WHERE user_id = ?";
            int updatedRows = jdbcTemplate.update(sql, now, userId);
            
            if (updatedRows != 1) {
                log.warn(">> 로그인 시도 횟수 초기화 실패: 업데이트된 행 수={}", updatedRows);
                return false;
            }
            
            // 사용자 이메일 조회 및 Redis에서 잠금 키 삭제
            String email = getUserEmail(userId);
            if (email != null) {
                unlockAccountInRedis(email);
            }
            
            log.info(">> 로그인 시도 횟수 초기화 성공: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error(">> 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
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
    }
    
    /**
     * 현재 로그인 시도 횟수 조회
     *
     * @param userId 사용자 ID
     * @return 현재 로그인 시도 횟수
     */
    public Integer getCurrentAttemptCount(Long userId) {
        try {
            String sql = "SELECT login_attempt_count FROM users WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            log.warn(">> 로그인 시도 횟수 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
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
            return null;
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
     * 계정 잠금 상태 확인
     * Redis에서 계정 잠금 키 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return true: 계정이 잠김, false: 계정이 잠기지 않음
     */
    public boolean isAccountLocked(Long userId) {
        if (userId == null) {
            return false;
        }
        
        // 사용자 이메일 조회
        String email = getUserEmail(userId);
        if (email == null) {
            log.warn(">> 계정 잠금 상태 확인 실패: 사용자 이메일을 찾을 수 없음, userId={}", userId);
            return false;
        }
        
        return isAccountLocked(email);
    }
    
    /**
     * 계정 잠금 시간 확인
     * 
     * @param userId 사용자 ID
     * @return 남은 잠금 시간(분), 잠금 시간이 지났거나 잠기지 않은 경우 0
     */
    public long getRemainingLockTime(Long userId) {
        try {
            // 사용자 이메일 조회
            String email = getUserEmail(userId);
            if (email == null) {
                return 0;
            }
            
            String key = ACCOUNT_LOCK_PREFIX + email;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.warn(">> 계정 잠금 시간 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 모든 사용자의 로그인 시도 횟수 초기화
     * 항상 새로운 트랜잭션에서 실행되어 DB에 즉시 반영됩니다.
     *
     * @return 초기화된 사용자 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int resetAllLoginAttempts() {
        try {
            log.info(">> 모든 사용자 로그인 시도 횟수 초기화 시작");
            
            // 단순 SQL 쿼리로 모든 사용자 초기화 (account_locked는 Redis로 관리하므로 업데이트 X)
            String sql = "UPDATE users SET login_attempt_count = 0";
            int updatedRows = jdbcTemplate.update(sql);
            
            // Redis에서 모든 계정 잠금 키 삭제
            Set<String> keys = redisTemplate.keys(ACCOUNT_LOCK_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info(">> Redis에서 모든 계정 잠금 키 삭제 완료: 키 수={}", keys.size());
            }
            
            log.info(">> 모든 사용자 로그인 시도 횟수 초기화 완료: 초기화된 계정 수={}", updatedRows);
            return updatedRows;
        } catch (Exception e) {
            log.error(">> 모든 사용자 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }
} 