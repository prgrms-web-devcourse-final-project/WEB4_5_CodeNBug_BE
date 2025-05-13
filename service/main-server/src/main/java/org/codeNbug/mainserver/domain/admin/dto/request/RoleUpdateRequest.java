package org.codeNbug.mainserver.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 역할 변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    
    /**
     * 변경할 역할 (USER, ADMIN, MANAGER)
     */
    @NotBlank(message = "역할을 입력해주세요.")
    @Pattern(regexp = "^(USER|ADMIN|MANAGER)$", message = "역할은 USER, ADMIN 또는 MANAGER 중 하나여야 합니다.")
    private String role;
} 