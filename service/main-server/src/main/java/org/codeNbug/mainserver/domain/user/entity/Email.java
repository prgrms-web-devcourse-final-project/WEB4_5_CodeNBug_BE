package org.codeNbug.mainserver.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "email")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_id", unique = true, nullable = false)
    private Long id;

    // 이메일 주소
    @Column(name = "email", nullable = false)
    private String email;

    // 이메일 인증 여부
    @Column(name = "email_status", nullable = false)
    private boolean emailStatus;

    @Builder
    public Email(String email) {
        this.email = email;
        this.emailStatus = false;
    }
}