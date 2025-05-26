package org.codenbug.user.sns.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sns_users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SnsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // AUTO_INCREMENT 적용

    @Column(name = "social_id", nullable = false, unique = true)
    private String socialId; // 소셜 로그인에서 받은 사용자 ID (예: Google ID, Kakao ID)

    @Column(name = "provider", nullable = false)
    private String provider; // 소셜 로그인 제공자 (Google, Kakao, Naver 등)

    @Column(name = "name", nullable = true)
    private String name; // 사용자 이름 (옵션)

    @Column(name = "created_at")
    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Timestamp updatedAt;

    @Column(name = "email")
    private String email; // 사용자 이메일

    @Column(name = "age")
    private Integer age;

    @Column(name = "sex")
    private String sex;

    @Column(name = "phone_num")
    private String phoneNum;

    @Column(name = "location")
    private String location;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "is_additional_info_completed")
    private Boolean isAdditionalInfoCompleted;

    @Column(name = "account_expired_at")
    private Timestamp accountExpiredAt;

    @Column(name = "account_locked")
    private boolean accountLocked;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "last_login_at")
    private Timestamp lastLoginAt;

    @Builder.Default
    @Column(name = "max_login_attempts")
    private int maxLoginAttempts = 5;

    @Builder.Default
    @Column(name = "account_lock_duration_minutes")
    private int accountLockDurationMinutes = 30;

    @Builder.Default
    @Column(name = "account_expiry_days")
    private int accountExpiryDays = 365;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.isAdditionalInfoCompleted = false;
        this.enabled = true;
        this.accountLocked = false;
        this.accountExpiredAt = new Timestamp(System.currentTimeMillis() + (accountExpiryDays * 24L * 60 * 60 * 1000));
    }
    
    /**
     * SNS 사용자 정보를 업데이트합니다.
     * 
     * @param name 새 이름
     * @param phoneNum 새 전화번호
     * @param location 새 위치
     */
    public void update(String name, Integer age, String sex, String phoneNum, String location) {
        this.name = name;
        this.age = age;
        this.sex = sex;
        this.phoneNum = phoneNum;
        this.location = location;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
}