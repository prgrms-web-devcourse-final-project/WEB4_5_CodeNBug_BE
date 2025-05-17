package org.codeNbug.mainserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인 시도 횟수 관리 서비스
 * JdbcTemplate을 사용해 데이터베이스에 직접 접근하여 로그인 시도 횟수를 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final JdbcTemplate jdbcTemplate;
    
    // 최대 로그인 시도 횟수 (5회)
    private static final int MAX_ATTEMPTS = 5;
    
    // 계정 잠금 지속 시간 (분)
    private static final int LOCK_DURATION_MINUTES = 3;

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
            
            // 최대 시도 횟수 초과 시 계정 잠금
            if (currentAttempts != null && currentAttempts >= MAX_ATTEMPTS) {
                lockAccount(userId);
                log.info(">> 계정이 잠겼습니다: userId={}, 시도 횟수={}/{}", userId, currentAttempts, MAX_ATTEMPTS);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error(">> 로그인 시도 횟수 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return false;
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
            
            // 단순 SQL 쿼리로 초기화
            String sql = "UPDATE users SET login_attempt_count = 0, account_locked = false, last_login_at = ? WHERE user_id = ?";
            int updatedRows = jdbcTemplate.update(sql, now, userId);
            
            if (updatedRows != 1) {
                log.warn(">> 로그인 시도 횟수 초기화 실패: 업데이트된 행 수={}", updatedRows);
                return false;
            }
            
            log.info(">> 로그인 시도 횟수 초기화 성공: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error(">> 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 계정 잠금 처리
     * 항상 새로운 트랜잭션에서 실행되어 DB에 즉시 반영됩니다.
     *
     * @param userId 사용자 ID
     * @return 잠금 성공 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean lockAccount(Long userId) {
        try {
            log.info(">> 계정 잠금 처리 시작: userId={}", userId);
            
            // 단순 SQL 쿼리로 계정 잠금
            String sql = "UPDATE users SET account_locked = true WHERE user_id = ?";
            int updatedRows = jdbcTemplate.update(sql, userId);
            
            if (updatedRows != 1) {
                log.warn(">> 계정 잠금 처리 실패: 업데이트된 행 수={}", updatedRows);
                return false;
            }
            
            log.info(">> 계정 잠금 처리 성공: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error(">> 계정 잠금 처리 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
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
     * 계정 잠금 상태 확인
     *
     * @param userId 사용자 ID
     * @return true: 계정이 잠김, false: 계정이 잠기지 않음
     */
    public boolean isAccountLocked(Long userId) {
        try {
            String sql = "SELECT account_locked FROM users WHERE user_id = ?";
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
        } catch (Exception e) {
            log.warn(">> 계정 잠금 상태 조회 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 계정 잠금 시간 확인
     * 
     * @param userId 사용자 ID
     * @return 남은 잠금 시간(분), 잠금 시간이 지났거나 잠기지 않은 경우 0
     */
    public long getRemainingLockTime(Long userId) {
        try {
            String sql = "SELECT last_login_at FROM users WHERE user_id = ? AND account_locked = true";
            LocalDateTime lastLoginAt = jdbcTemplate.queryForObject(sql, LocalDateTime.class, userId);
            
            if (lastLoginAt == null) {
                return 0;
            }
            
            LocalDateTime lockExpiryTime = lastLoginAt.plusMinutes(LOCK_DURATION_MINUTES);
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), lockExpiryTime).toMinutes();
            
            return Math.max(0, remainingMinutes);
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
            
            // 단순 SQL 쿼리로 모든 사용자 초기화
            String sql = "UPDATE users SET login_attempt_count = 0, account_locked = false";
            int updatedRows = jdbcTemplate.update(sql);
            
            log.info(">> 모든 사용자 로그인 시도 횟수 초기화 완료: 초기화된 계정 수={}", updatedRows);
            return updatedRows;
        } catch (Exception e) {
            log.error(">> 모든 사용자 로그인 시도 횟수 초기화 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 오래된 계정 잠금 자동 해제
     * 잠금 시간이 지난 계정을 자동으로 해제합니다.
     * 항상 새로운 트랜잭션에서 실행되어 DB에 즉시 반영됩니다.
     *
     * @return 해제된 계정 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int unlockExpiredAccounts() {
        try {
            log.info(">> 오래된 계정 잠금 자동 해제 시작");
            
            LocalDateTime unlockTime = LocalDateTime.now().minusMinutes(LOCK_DURATION_MINUTES);
            
            // 잠금 시간이 지난 계정 해제
            String sql = "UPDATE users SET account_locked = false, login_attempt_count = 0 " +
                         "WHERE account_locked = true AND last_login_at <= ?";
            int updatedRows = jdbcTemplate.update(sql, unlockTime);
            
            log.info(">> 오래된 계정 잠금 자동 해제 완료: 해제된 계정 수={}", updatedRows);
            return updatedRows;
        } catch (Exception e) {
            log.error(">> 오래된 계정 잠금 자동 해제 중 오류 발생: {}", e.getMessage(), e);
            return 0;
        }
    }
} 