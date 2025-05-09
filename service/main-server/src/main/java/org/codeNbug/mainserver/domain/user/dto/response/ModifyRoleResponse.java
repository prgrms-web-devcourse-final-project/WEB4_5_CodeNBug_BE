package org.codeNbug.mainserver.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyRoleResponse {
    private String role;

    /**
     * 역할 정보로 ModifyRoleResponse 생성
     *
     * @param role 역할
     * @return ModifyRoleResponse 객체
     */
    public static ModifyRoleResponse of(String role) {
        return ModifyRoleResponse.builder()
                .role(role)
                .build();
    }
}
