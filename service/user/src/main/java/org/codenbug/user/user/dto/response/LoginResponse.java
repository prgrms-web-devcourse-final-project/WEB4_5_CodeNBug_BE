package org.codenbug.user.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 값은 JSON에서 제외
public class LoginResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;

    /**
     * 토큰 정보로 LoginResponse 생성
     *
     * @param accessToken  액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return LoginResponse 객체
     */
    public static LoginResponse of(String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 토큰 타입만 포함된 LoginResponse 생성
     *
     * @return LoginResponse 객체
     */
    public static LoginResponse ofTokenTypeOnly() {
        return LoginResponse.builder()
                .tokenType("Bearer")
                .build();
    }
} 