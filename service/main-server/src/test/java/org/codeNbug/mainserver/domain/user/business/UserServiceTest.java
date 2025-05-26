package org.codeNbug.mainserver.domain.user.business;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.domain.user.service.LoginAttemptService;
import org.codeNbug.mainserver.domain.user.service.UserService;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.codenbug.user.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String testEmail = "test@example.com";
    private String testPassword = "Test1234!";

    private Validator validator;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email(testEmail)
                .password(passwordEncoder.encode(testPassword))
                .name("테스트")
                .age(25)
                .sex("남성")
                .phoneNum("010-1234-5678")
                .location("서울시 강남구")
                .role("ROLE_USER")
                .build();

        // CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        // 인증 토큰 생성 및 SecurityContext에 등록
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Validator 설정
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {
        @Test
        @DisplayName("유효한 회원가입 요청시 성공")
        void 회원가입_성공() {
            // given
            SignupRequest request = createValidSignupRequest();
            User newUser = User.builder()
                    .userId(2L)
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .age(request.getAge())
                    .sex(request.getSex())
                    .phoneNum(request.getPhoneNum())
                    .location(request.getLocation())
                    .role("ROLE_USER")
                    .build();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            // when
            SignupResponse response = userService.signup(request);

            // then
            assertNotNull(response);
            assertEquals(request.getEmail(), response.getEmail());
            assertEquals(request.getName(), response.getName());
            assertEquals(request.getAge(), response.getAge());
            assertEquals(request.getSex(), response.getSex());
            assertEquals(request.getPhoneNum(), response.getPhoneNum());
            assertEquals(request.getLocation(), response.getLocation());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("중복된 이메일로 회원가입시 실패")
        void 회원가입_중복_이메일() {
            // given
            SignupRequest request = createValidSignupRequest();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            // when & then
            assertThrows(DuplicateEmailException.class, () -> userService.signup(request));
        }

        @Test
        @DisplayName("유효하지 않은 이메일 형식으로 회원가입시 실패")
        void 회원가입_유효하지않은_이메일() {
            // given
            SignupRequest request = SignupRequest.builder()
                    .email("invalid-email")
                    .password("New1234!")
                    .name("신규사용자")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9876-5432")
                    .location("서울시 서초구")
                    .build();

            // when & then
            assertThrows(NullPointerException.class, () -> userService.signup(request));
        }

        @Test
        @DisplayName("유효하지 않은 비밀번호 형식으로 회원가입시 실패")
        void 회원가입_유효하지않은_비밀번호() {
            // given
            SignupRequest request = SignupRequest.builder()
                    .email("new@example.com")
                    .password("weak")
                    .name("신규사용자")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9876-5432")
                    .location("서울시 서초구")
                    .build();

            // when & then
            assertThrows(NullPointerException.class, () -> userService.signup(request));
        }

        @Test
        @DisplayName("유효하지 않은 전화번호 형식으로 회원가입시 실패")
        void 회원가입_유효하지않은_전화번호() {
            // given
            SignupRequest request = SignupRequest.builder()
                    .email("new@example.com")
                    .password("New1234!")
                    .name("신규사용자")
                    .age(30)
                    .sex("여성")
                    .phoneNum("1234")
                    .location("서울시 서초구")
                    .build();

            // when & then
            assertThrows(NullPointerException.class, () -> userService.signup(request));
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {
        @Test
        @DisplayName("유효한 로그인 요청시 성공")
        void 로그인_성공() {
            // given
            LoginRequest request = new LoginRequest(testEmail, testPassword);
            TokenService.TokenInfo tokenInfo = new TokenService.TokenInfo("accessToken", "refreshToken");
            
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
            when(tokenService.generateTokens(testUser.getEmail())).thenReturn(tokenInfo);
            when(loginAttemptService.isAccountLocked(request.getEmail())).thenReturn(false);

            // when
            LoginResponse response = userService.login(request);

            // then
            assertNotNull(response);
            assertEquals(tokenInfo.getAccessToken(), response.getAccessToken());
            assertEquals(tokenInfo.getRefreshToken(), response.getRefreshToken());
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인시 실패")
        void 로그인_이메일_없음() {
            // given
            LoginRequest request = new LoginRequest("nonexistent@example.com", testPassword);
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            // No need to mock loginAttemptService as it's never called when user doesn't exist

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.login(request));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인시 실패")
        void 로그인_비밀번호_불일치() {
            // given
            LoginRequest request = new LoginRequest(testEmail, "wrongPassword");
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(false);
            when(loginAttemptService.isAccountLocked(request.getEmail())).thenReturn(false);

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.login(request));
        }

        @Test
        @DisplayName("계정이 잠긴 상태로 로그인시 실패")
        void 로그인_계정_잠김() {
            // given
            LoginRequest request = new LoginRequest(testEmail, testPassword);
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
            when(loginAttemptService.isAccountLocked(request.getEmail())).thenReturn(true);

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.login(request));
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {
        @Test
        @DisplayName("로그아웃 성공")
        void 로그아웃_성공() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            String email = testUser.getEmail();

            when(tokenService.getSubjectFromToken(refreshToken)).thenReturn(email);
            when(tokenService.getSubjectFromToken(accessToken)).thenReturn(email);
            when(tokenService.getExpirationTimeFromToken(accessToken)).thenReturn(3600L);

            // when
            userService.logout(accessToken, refreshToken);

            // then
            verify(tokenService).deleteRefreshToken(email);
            verify(tokenService).addToBlacklist(accessToken, 3600L);
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 로그아웃시 실패")
        void 로그아웃_유효하지않은_리프레시토큰() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "invalid-refresh-token";

            when(tokenService.getSubjectFromToken(refreshToken)).thenReturn(null);

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.logout(accessToken, refreshToken));
        }

        @Test
        @DisplayName("유효하지 않은 액세스 토큰으로 로그아웃시 실패")
        void 로그아웃_유효하지않은_액세스토큰() {
            // given
            String accessToken = "invalid-access-token";
            String refreshToken = "test-refresh-token";

            when(tokenService.getSubjectFromToken(refreshToken)).thenReturn(testUser.getEmail());
            when(tokenService.getSubjectFromToken(accessToken)).thenReturn(null);

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.logout(accessToken, refreshToken));
        }
    }

    @Nested
    @DisplayName("프로필 조회 테스트")
    class ProfileTest {
        @Test
        @DisplayName("프로필 조회 성공")
        void 프로필_조회_성공() {
            // when
            UserProfileResponse response = userService.getProfile();

            // then
            assertNotNull(response);
            assertEquals(testUser.getEmail(), response.getEmail());
            assertEquals(testUser.getName(), response.getName());
            assertEquals(testUser.getAge(), response.getAge());
            assertEquals(testUser.getSex(), response.getSex());
            assertEquals(testUser.getPhoneNum(), response.getPhoneNum());
            assertEquals(testUser.getLocation(), response.getLocation());
        }

        @Test
        @DisplayName("인증되지 않은 사용자의 프로필 조회시 실패")
        void 프로필조회_인증되지않은_사용자() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThrows(AuthenticationFailedException.class, () -> userService.getProfile());
        }
    }

    @Nested
    @DisplayName("프로필 수정 테스트")
    class ProfileUpdateTest {
        @Test
        @DisplayName("프로필 수정 성공")
        void 프로필_수정_성공() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("서울시 송파구")
                    .build();

            // when
            UserProfileResponse response = userService.updateProfile(request);

            // then
            assertNotNull(response);
            assertEquals(request.getName(), response.getName());
            assertEquals(request.getAge(), response.getAge());
            assertEquals(request.getSex(), response.getSex());
            assertEquals(request.getPhoneNum(), response.getPhoneNum());
            assertEquals(request.getLocation(), response.getLocation());
        }

        @Test
        @DisplayName("이름이 2자 미만인 경우 실패")
        void 프로필수정_이름_길이_부족() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("김")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("서울시 송파구")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> 
                violation.getPropertyPath().toString().equals("name") &&
                violation.getMessage().contains("2자 이상 50자 이하여야 합니다")));
        }

        @Test
        @DisplayName("이름이 50자를 초과하는 경우 실패")
        void 프로필수정_이름_길이_초과() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("가".repeat(51))
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("서울시 송파구")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> 
                violation.getPropertyPath().toString().equals("name") &&
                violation.getMessage().contains("2자 이상 50자 이하여야 합니다")));
        }

        @Test
        @DisplayName("전화번호 형식이 잘못된 경우 실패")
        void 프로필수정_잘못된_전화번호() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(30)
                    .sex("여성")
                    .phoneNum("1234")
                    .location("서울시 송파구")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> 
                violation.getPropertyPath().toString().equals("phoneNum") &&
                violation.getMessage().contains("전화번호 형식이 올바르지 않습니다")));
        }

        @Test
        @DisplayName("주소가 2자 미만인 경우 실패")
        void 프로필수정_주소_길이_부족() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("서")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> 
                violation.getPropertyPath().toString().equals("location") &&
                violation.getMessage().contains("2자 이상 100자 이하여야 합니다")));
        }

        @Test
        @DisplayName("주소가 100자를 초과하는 경우 실패")
        void 프로필수정_주소_길이_초과() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(30)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("가".repeat(101))
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> 
                violation.getPropertyPath().toString().equals("location") &&
                violation.getMessage().contains("2자 이상 100자 이하여야 합니다")));
        }

        @Test
        @DisplayName("나이가 null인 경우 실패")
        void 프로필수정_나이_null() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(null)
                    .sex("여성")
                    .phoneNum("010-9999-8888")
                    .location("서울시 송파구")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation ->
                    violation.getPropertyPath().toString().equals("age") &&
                            violation.getMessage().contains("나이는 필수 입력 항목입니다")));
        }

        @Test
        @DisplayName("성별이 빈 값인 경우 실패")
        void 프로필수정_성별_빈값() {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .name("수정된이름")
                    .age(30)
                    .sex("")
                    .phoneNum("010-9999-8888")
                    .location("서울시 송파구")
                    .build();

            // when
            Set<jakarta.validation.ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation ->
                    violation.getPropertyPath().toString().equals("sex") &&
                            violation.getMessage().contains("성별은 필수 입력 항목입니다")));
        }
    }

    // Helper methods
    private SignupRequest createValidSignupRequest() {
        return SignupRequest.builder()
                .email("new@example.com")
                .password("New1234!")
                .name("신규사용자")
                .age(30)
                .sex("여성")
                .phoneNum("010-9876-5432")
                .location("서울시 서초구")
                .build();
    }
} 