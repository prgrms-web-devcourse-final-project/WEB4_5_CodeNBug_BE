package org.codenbug.user.domain.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User 엔티티 클래스
 */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "sex", nullable = false)
    private String sex;

    @Column(name = "phoneNum", length = 255)
    private String phoneNum;

    @Column(name = "addresses", length = 255)
    private String location;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "age", nullable = false)
    private Integer age;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(name = "account_expired_at")
    private LocalDateTime accountExpiredAt;

    @Builder.Default
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "password_expired_at")
    private LocalDateTime passwordExpiredAt;

    @Builder.Default
    @Column(name = "login_attempt_count", nullable = false)
    private Integer loginAttemptCount = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "max_login_attempts")
    private Integer maxLoginAttempts = 5;

    @Builder.Default
    @Column(name = "account_lock_duration_minutes")
    private Integer accountLockDurationMinutes = 5;

    @Builder.Default
    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays = 90;

    @Builder.Default
    @Column(name = "account_expiry_days")
    private Integer accountExpiryDays = 365;

    /*@Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();*/

    /**
     * 사용자 정보를 업데이트합니다.
     */
    public void update(String name, Integer age, String sex, String phoneNum, String location) {
        this.name = name;
        this.age = age;
        this.sex = sex;
        this.phoneNum = phoneNum;
        this.location = location;
    }

    /**
     * 사용자의 역할을 업데이트합니다.
     * 
     * @param role 변경할 역할
     */
    public void updateRole(String role) {
        this.role = role;
    }

    public void setLoginAttemptCount(int loginAttemptCount) {
        this.loginAttemptCount = loginAttemptCount;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public void setAccountExpiredAt(LocalDateTime accountExpiredAt) {
        this.accountExpiredAt = accountExpiredAt;
    }

    public void setPasswordExpiredAt(LocalDateTime passwordExpiredAt) {
        this.passwordExpiredAt = passwordExpiredAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 계정 잠금 상태를 확인합니다.
     * 실제 계정 잠금 상태는 Redis와 데이터베이스 모두에서 관리됩니다.
     * 이 메서드는 데이터베이스의 accountLocked 필드 상태만 반환합니다.
     * 
     * 참고: 완전한 계정 잠금 상태 확인은 LoginAttemptService.isAccountLocked()를 사용하세요.
     * 이 메서드는 DB의 상태만 확인하며, UI 표시 등의 목적으로 사용됩니다.
     */
    public boolean isAccountLocked() {
        return accountLocked != null && accountLocked;
    }

    public boolean isEnabled() {
        return enabled != null && enabled;
    }
}
//