package org.codeNbug.mainserver.domain.user.dto.response;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.sns.Entity.SnsUser;

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
    private Boolean isSnsUser;
    private String provider;

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
                .isSnsUser(false)
                .build();
    }
    
    /**
     * SnsUser 엔티티를 UserProfileResponse DTO로 변환
     *
     * @param snsUser SnsUser 엔티티
     * @return UserProfileResponse DTO
     */
    public static UserProfileResponse fromSnsEntity(SnsUser snsUser) {
        return UserProfileResponse.builder()
                .id(snsUser.getId())
                .email(snsUser.getEmail())
                .name(snsUser.getName())
                .sex(snsUser.getSex())
                .age(snsUser.getAge())
                .phoneNum(snsUser.getPhoneNum())
                .location(snsUser.getLocation())
                .role("ROLE_USER") // SNS 사용자는 기본적으로 USER 역할을 가짐
                .createdAt(convertTimestamp(snsUser.getCreatedAt()))
                .modifiedAt(convertTimestamp(snsUser.getUpdatedAt()))
                .isSnsUser(true)
                .provider(snsUser.getProvider())
                .build();
    }
    
    /**
     * Timestamp를 LocalDateTime으로 변환
     */
    private static LocalDateTime convertTimestamp(Timestamp timestamp) {
        return timestamp != null 
               ? timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() 
               : null;
    }
} 