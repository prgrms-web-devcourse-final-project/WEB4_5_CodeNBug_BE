package org.codeNbug.mainserver.domain.user.dto.response;

import java.time.LocalDateTime;

import org.codenbug.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String sex;
    private Integer age;
    private String phoneNum;
    private String location;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /**
     * User 엔티티를 UserProfileResponse DTO로 변환
     *
     * @param user User 엔티티
     * @return UserProfileResponse DTO
     */
    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .sex(user.getSex())
                .age(user.getAge())
                .phoneNum(user.getPhoneNum())
                .location(user.getLocation())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .modifiedAt(user.getModifiedAt())
                .build();
    }
} 