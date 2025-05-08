package org.codenbug.user.sns.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private String name;
    private String accessToken;
    private String refreshToken;
    private String provider;
    private String socialId;

    public UserResponse(String name, String accessToken, String refreshToken, String provider) {
        this.name = name;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.provider = provider;
    }
    
    public UserResponse(String name, String accessToken, String refreshToken, String provider, String socialId) {
        this.name = name;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.provider = provider;
        this.socialId = socialId;
    }
    
    // 토큰 타입만 포함된 응답 생성 (액세스 토큰 및 리프레시 토큰 값은 제외)
    public static UserResponse ofTokenTypeOnly() {
        return new UserResponse(null, "Bearer", "Bearer", null);
    }
}
