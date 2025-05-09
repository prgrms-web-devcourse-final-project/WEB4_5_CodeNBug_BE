package org.codeNbug.mainserver.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.junit.jupiter.api.AfterEach;
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
	private String testEmail = "test@example.com";
	private String testPassword = "Test1234!";
	private TokenService.TokenInfo tokenInfo;

	@BeforeEach
	void setUp() {
		// 테스트용 사용자 생성
		testUser = User.builder()
			.email(testEmail)
			.password(passwordEncoder.encode(testPassword))
			.name("테스트")
			.age(25)
			.sex("남성")
			.phoneNum("010-1234-5678")
			.location("서울시 강남구")
			.role("ROLE_USER")
			.build();
		userRepository.save(testUser);

		// 테스트용 토큰 생성 - 각 테스트에서 필요할 때 생성하도록 변경
		tokenInfo = tokenService.generateTokens(testUser.getEmail());
		testToken = tokenInfo.getAccessToken();
	}

	@AfterEach
	void tearDown() {
		// 토큰 정보 정리
		if (tokenInfo != null && tokenInfo.getAccessToken() != null) {
			tokenService.deleteRefreshToken(testUser.getEmail());
		}
		
		// 테스트 사용자 삭제 - @Transactional로 롤백되지만 추가 보장
		userRepository.deleteAll();
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
		LoginRequest loginRequest = LoginRequest.builder().email(testEmail).password(testPassword).build();

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
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(freshTokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			post("/api/v1/users/logout").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("로그아웃 성공"));
	}

	@Test
	@DisplayName("로그아웃 실패 테스트 - 리프레시 토큰 없음")
	void logout_fail_no_refresh_token() throws Exception {
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		
		// when - 리프레시 토큰을 의도적으로 전달하지 않음
		ResultActions result = mockMvc.perform(
			post("/api/v1/users/logout").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken()));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("인증 정보가 필요합니다. 다시 로그인해주세요."));
	}

	@Test
	@DisplayName("회원 탈퇴 성공 테스트")
	void withdrawal_success() throws Exception {
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(freshTokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			delete("/api/v1/users/me").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken())
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
			.email(testEmail)  // 이미 존재하는 이메일
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
			.email(testEmail)
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

	@Test
	@DisplayName("프로필 조회 성공 테스트")
	void getProfile_success() throws Exception {
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(freshTokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			get("/api/v1/users/me").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("프로필 조회 성공"))
			.andExpect(jsonPath("$.data.email").value(testEmail))
			.andExpect(jsonPath("$.data.name").value("테스트"))
			.andExpect(jsonPath("$.data.age").value(25))
			.andExpect(jsonPath("$.data.sex").value("남성"))
			.andExpect(jsonPath("$.data.phoneNum").value("010-1234-5678"))
			.andExpect(jsonPath("$.data.location").value("서울시 강남구"));
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 프로필 조회 시도 테스트")
	void getProfile_unauthorized() throws Exception {
		// given
		String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // header
			+ ".eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ"  // payload
			+ ".invalid_signature";  // 잘못된 signature

		// when
		ResultActions result = mockMvc.perform(
			get("/api/v1/users/me").header("Authorization", "Bearer " + invalidToken));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("인증 정보가 필요합니다."));
	}

	@Test
	@DisplayName("프로필 수정 성공 테스트")
	void updateProfile_success() throws Exception {
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(freshTokenInfo.getRefreshToken());
		UserUpdateRequest updateRequest = UserUpdateRequest.builder()
			.name("수정된이름")
			.phoneNum("010-9999-8888")
			.location("서울시 송파구")
			.build();

		// when
		ResultActions result = mockMvc.perform(
			put("/api/v1/users/me").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken())
				.cookie(refreshTokenCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("프로필 수정 성공"))
			.andExpect(jsonPath("$.data.name").value("수정된이름"))
			.andExpect(jsonPath("$.data.phoneNum").value("010-9999-8888"))
			.andExpect(jsonPath("$.data.location").value("서울시 송파구"));
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 프로필 수정 시도 테스트")
	void updateProfile_unauthorized() throws Exception {
		// given
		String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // header
			+ ".eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ"  // payload
			+ ".invalid_signature";  // 잘못된 signature

		UserUpdateRequest updateRequest = UserUpdateRequest.builder()
			.name("수정된이름")
			.phoneNum("010-9999-8888")
			.location("서울시 송파구")
			.build();

		// when
		ResultActions result = mockMvc.perform(
			put("/api/v1/users/me").header("Authorization", "Bearer " + invalidToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("인증 정보가 필요합니다."));
	}

	@Test
	@DisplayName("잘못된 형식의 프로필 수정 요청 테스트")
	void updateProfile_badRequest() throws Exception {
		// given - 각 테스트마다 새로운 토큰 생성
		TokenService.TokenInfo freshTokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(freshTokenInfo.getRefreshToken());
		// 빈 이름으로 요청 (validation 실패 예상)
		UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
			.name("")
			.phoneNum("010-9999-8888")
			.location("서울시 송파구")
			.build();

		// when
		ResultActions result = mockMvc.perform(
			put("/api/v1/users/me").header("Authorization", "Bearer " + freshTokenInfo.getAccessToken())
				.cookie(refreshTokenCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)));

		// then
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("데이터 형식이 잘못되었습니다."));
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