package org.codeNbug.mainserver.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    /*@Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();*/

    /**
     * 사용자 정보를 업데이트합니다.
     *
     * @param name 새로운 이름 (null인 경우 업데이트하지 않음)
     * @param phoneNum 새로운 전화번호 (null인 경우 업데이트하지 않음)
     * @param location 새로운 주소 (null인 경우 업데이트하지 않음)
     */
    public void update(String name, String phoneNum, String location) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.location = location;
    }
}