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

    @Column(name = "account_locked")
    private boolean accountLocked;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "password_expired_at")
    private LocalDateTime passwordExpiredAt;

    @Column(name = "login_attempt_count")
    private int loginAttemptCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "max_login_attempts")
    private int maxLoginAttempts = 5;

    @Builder.Default
    @Column(name = "account_lock_duration_minutes")
    private int accountLockDurationMinutes = 5;

    @Builder.Default
    @Column(name = "password_expiry_days")
    private int passwordExpiryDays = 90;

    @Builder.Default
    @Column(name = "account_expiry_days")
    private int accountExpiryDays = 365;

    /*@Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();*/

    /**
     * 사용자 정보를 업데이트합니다.
     */
    public void update(String name, String phoneNum, String location) {
        this.name = name;
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
}
//