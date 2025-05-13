package org.codeNbug.mainserver.domain.admin.dto.response;

import java.time.LocalDateTime;

import org.codenbug.user.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 회원가입 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSignupResponse {
    private Long id;
    private String email;
    private String name;
    private Integer age;
    private String sex;
    private String phoneNum;
    private String location;
    private String role;
    private LocalDateTime createdAt;

    /**
     * User 엔티티를 AdminSignupResponse DTO로 변환
     *
     * @param user User 엔티티
     * @return AdminSignupResponse DTO
     */
    public static AdminSignupResponse fromEntity(User user) {
        return AdminSignupResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .age(user.getAge())
                .sex(user.getSex())
                .phoneNum(user.getPhoneNum())
                .location(user.getLocation())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
} 