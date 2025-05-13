package org.codeNbug.mainserver.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 역할 변경 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyRoleResponse {
    
    /**
     * 변경된 역할
     */
    private String role;
    
    /**
     * 역할이 담긴 응답 객체 생성
     * 
     * @param role 역할
     * @return 응답 객체
     */
    public static ModifyRoleResponse of(String role) {
        return ModifyRoleResponse.builder()
                .role(role)
                .build();
    }
} 