package org.codeNbug.mainserver.external.sns.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.sql.Timestamp;

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
    @CreatedDate
    private Timestamp createdAt;

    @Column(name = "updated_at")
    @CreatedDate
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

    @Column(name = "is_additional_info_completed")
    private Boolean isAdditionalInfoCompleted;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.isAdditionalInfoCompleted = false;
    }
    
    /**
     * SNS 사용자 정보를 업데이트합니다.
     * 
     * @param name 새 이름
     * @param phoneNum 새 전화번호
     * @param location 새 위치
     */
    public void update(String name, String phoneNum, String location) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.location = location;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
}