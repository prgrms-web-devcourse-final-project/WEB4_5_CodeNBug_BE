package org.codeNbug.mainserver.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private CookieUtil cookieUtil;

	private String testToken;
	private User testUser;

	@BeforeEach
	void setUp() {
		// 테스트용 사용자 생성
		testUser = User.builder()
			.email("test@example.com")
			.password(passwordEncoder.encode("Test1234!"))
			.name("테스트")
			.age(25)
			.sex("남성")
			.phoneNum("010-1234-5678")
			.location("서울시 강남구")
			.role("ROLE_USER")
			.build();
		userRepository.save(testUser);

		// 테스트용 토큰 생성
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		testToken = tokenInfo.getAccessToken();
	}

	@Test
	@DisplayName("회원가입 성공 테스트")
	void signup_success() throws Exception {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.email("new@example.com")
			.password("New1234!")
			.name("신규사용자")
			.age(30)
			.sex("여성")
			.phoneNum("010-9876-5432")
			.location("서울시 서초구")
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(signupRequest)));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("회원가입 성공"))
			.andExpect(jsonPath("$.data.email").value("new@example.com"))
			.andExpect(jsonPath("$.data.name").value("신규사용자"))
			.andExpect(jsonPath("$.data.age").value(30))
			.andExpect(jsonPath("$.data.sex").value("여성"))
			.andExpect(jsonPath("$.data.phoneNum").value("010-9876-5432"))
			.andExpect(jsonPath("$.data.location").value("서울시 서초구"));
	}

	@Test
	@DisplayName("로그인 성공 테스트")
	void login_success() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder().email("test@example.com").password("Test1234!").build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest)));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("로그인 성공"));
	}

	@Test
	@DisplayName("로그아웃 성공 테스트")
	void logout_success() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = generateTestTokens();
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			post("/api/v1/users/logout").header("Authorization", "Bearer " + tokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("로그아웃 성공"));
	}

	@Test
	@DisplayName("로그아웃 실패 테스트 - 리프레시 토큰 없음")
	void logout_fail_no_refresh_token() throws Exception {
		// when
		ResultActions result = mockMvc.perform(
			post("/api/v1/users/logout").header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("인증 정보가 필요합니다. 다시 로그인해주세요."));
	}

	@Test
	@DisplayName("회원 탈퇴 성공 테스트")
	void withdrawal_success() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = generateTestTokens();
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			delete("/api/v1/users/me").header("Authorization", "Bearer " + tokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("회원 탈퇴 성공"));
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 회원 탈퇴 시도 테스트")
	void withdrawal_unauthorized() throws Exception {
		// given
		String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // header
			+ ".eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ"  // payload
			+ ".invalid_signature";  // 잘못된 signature

		// when
		ResultActions result = mockMvc.perform(
			delete("/api/v1/users/me").header("Authorization", "Bearer " + invalidToken));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("인증 정보가 필요합니다."));
	}

	@Test
	@DisplayName("중복된 이메일로 회원가입 시도 테스트")
	void signup_duplicate_email() throws Exception {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.email("test@example.com")  // 이미 존재하는 이메일
			.password("Test1234!")
			.name("중복사용자")
			.age(25)
			.sex("남성")
			.phoneNum("010-1111-2222")
			.location("서울시 강남구")
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(signupRequest)));

		// then
		result.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("409-CONFLICT"))
			.andExpect(jsonPath("$.msg").value("이미 존재하는 이메일입니다."));
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인 시도 테스트")
	void login_wrong_password() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.email("test@example.com")
			.password("WrongPassword123!")
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest)));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요."));
	}

	/**
	 * 테스트용 토큰 생성 헬퍼 메서드
	 * @return TokenInfo 객체
	 */
	private TokenService.TokenInfo generateTestTokens() {
		return tokenService.generateTokens(testUser.getEmail());
	}

	/**
	 * 테스트용 리프레시 토큰 쿠키 생성 헬퍼 메서드
	 * @param refreshToken 리프레시 토큰
	 * @return Cookie 객체
	 */
	private Cookie createTestRefreshTokenCookie(String refreshToken) {
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		refreshTokenCookie.setHttpOnly(true);
		refreshTokenCookie.setSecure(false);  // 테스트 환경에서는 false로 설정
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
		return refreshTokenCookie;
	}
}