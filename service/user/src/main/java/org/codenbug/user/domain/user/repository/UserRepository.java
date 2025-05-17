package org.codenbug.user.domain.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.user.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 이메일로 사용자 조회
     *
     * @param email 조회할 사용자의 이메일
     * @return Optional<User> 조회된 사용자 (없으면 빈 Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 확인할 이메일
     * @return boolean 이메일이 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.location WHERE u.email = :email")
    Optional<User> findByEmailWithAddresses(@Param("email") String email);

    /**
     * 잠긴 계정 목록을 조회합니다.
     *
     * @return 잠긴 계정 목록
     */
    List<User> findByAccountLockedTrue();

    /**
     * 계정 만료일이 지정된 기간 내에 있는 사용자 목록을 조회합니다.
     *
     * @param start 시작일
     * @param end 종료일
     * @return 만료 예정인 계정 목록
     */
    @Query("SELECT u FROM User u WHERE u.accountExpiredAt BETWEEN :start AND :end")
    List<User> findByAccountExpiredAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 비밀번호 만료일이 지정된 기간 내에 있는 사용자 목록을 조회합니다.
     *
     * @param start 시작일
     * @param end 종료일
     * @return 만료 예정인 비밀번호를 가진 계정 목록
     */
    @Query("SELECT u FROM User u WHERE u.passwordExpiredAt BETWEEN :start AND :end")
    List<User> findByPasswordExpiredAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 로그인 시도 횟수가 null인 사용자 목록을 조회합니다.
     *
     * @return 로그인 시도 횟수가 null인 사용자 목록
     */
    List<User> findByLoginAttemptCountIsNull();

    /**
     * 사용자의 로그인 시도 횟수를 증가시키는 네이티브 쿼리입니다.
     * 
     * @param userId 사용자 ID
     * @param lastLoginAt 마지막 로그인 시도 시간
     * @return 업데이트된 행의 수
     */
    @Modifying
    @Query(value = "UPDATE users SET login_attempt_count = COALESCE(login_attempt_count, 0) + 1, last_login_at = :lastLoginAt WHERE user_id = :userId", nativeQuery = true)
    int incrementLoginAttemptCount(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    /**
     * 사용자의 계정 잠금 상태를 업데이트하는 네이티브 쿼리입니다.
     * 
     * @param userId 사용자 ID
     * @param locked 잠금 상태
     * @return 업데이트된 행의 수
     */
    @Modifying
    @Query(value = "UPDATE users SET account_locked = :locked WHERE user_id = :userId", nativeQuery = true)
    int updateAccountLockStatus(@Param("userId") Long userId, @Param("locked") boolean locked);
    
    /**
     * 사용자의 로그인 시도 횟수를 초기화하는 네이티브 쿼리입니다.
     * 
     * @param userId 사용자 ID
     * @param lastLoginAt 마지막 로그인 시도 시간
     * @return 업데이트된 행의 수
     */
    @Modifying
    @Query(value = "UPDATE users SET login_attempt_count = 0, account_locked = false, last_login_at = :lastLoginAt WHERE user_id = :userId", nativeQuery = true)
    int resetLoginAttemptCount(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 사용자의 로그인 시도 횟수를 강제로 초기화합니다 (관리자용)
     * 
     * @param userId 사용자 ID
     * @return 성공적으로 업데이트된 행의 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users SET login_attempt_count = 0 WHERE user_id = :userId", nativeQuery = true)
    int forceResetLoginAttemptCount(@Param("userId") Long userId);
    
    /**
     * 사용자의 로그인 시도 횟수를 강제로 설정합니다 (테스트용)
     * 
     * @param userId 사용자 ID
     * @param count 설정할 시도 횟수
     * @return 성공적으로 업데이트된 행의 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users SET login_attempt_count = :count WHERE user_id = :userId", nativeQuery = true)
    int forceSetLoginAttemptCount(@Param("userId") Long userId, @Param("count") int count);
}
