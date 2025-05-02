package org.codeNbug.mainserver.external.sns.Entity;

import jakarta.persistence.*;
import lombok.*;

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
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}