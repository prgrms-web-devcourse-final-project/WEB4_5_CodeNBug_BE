package org.codeNbug.mainserver.domain.user.dto.response;

import java.time.LocalDateTime;

import org.codenbug.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private Long id;
    private String email;
    private String name;
    private Integer age;
    private String sex;
    private String phoneNum;
    private String location;
    private LocalDateTime createdAt;

    /**
     * User 엔티티를 SignupResponse DTO로 변환
     *
     * @param user User 엔티티
     * @return SignupResponse DTO
     */
    public static SignupResponse fromEntity(User user) {
        return SignupResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .age(user.getAge())
                .sex(user.getSex())
                .phoneNum(user.getPhoneNum())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .build();
    }
} 