package org.codeNbug.mainserver.domain.user.dto.request;

import org.codenbug.user.domain.user.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    private String name;

    @NotNull(message = "나이는 필수 입력 항목입니다.")
    private Integer age;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    private String sex;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phoneNum;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    private String location;

    /**
     * SignupRequest를 User 엔티티로 변환
     *
     * @param passwordEncoder 비밀번호 인코더
     * @return User 엔티티
     */
    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password))
                .name(this.name)
                .age(this.age)
                .sex(this.sex)
                .phoneNum(this.phoneNum)
                .location(this.location)
                .role("ROLE_USER")
                .build();
    }
} 