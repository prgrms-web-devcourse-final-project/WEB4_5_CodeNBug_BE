package org.codeNbug.mainserver.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    @NotNull
    private String name;

    @NotNull(message = "나이는 필수 입력 항목입니다.")
    private Integer age;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    private String sex;

    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    @NotNull
    private String phoneNum;

    @Size(min = 2, max = 100, message = "주소는 2자 이상 100자 이하여야 합니다.")
    @NotNull
    private String location;
} 