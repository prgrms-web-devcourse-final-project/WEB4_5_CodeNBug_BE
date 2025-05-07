package org.codeNbug.mainserver.external.sns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoRequest {
    @NotNull(message = "나이는 필수 입력 항목입니다.")
    private Integer age;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    private String sex;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phoneNum;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    private String location;
} 