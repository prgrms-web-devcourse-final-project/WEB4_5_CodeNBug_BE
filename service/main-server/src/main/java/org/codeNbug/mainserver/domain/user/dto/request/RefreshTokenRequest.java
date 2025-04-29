package org.codeNbug.mainserver.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {
    private String email;
    private String refreshToken;
} 