package org.codeNbug.mainserver.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.util.TestUtil;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.common.util.JwtConfig;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Testcontainers
class UserControllerTest {
	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql =
		new MySQLContainer<>("mysql:8.0.34")
			.withDatabaseName("ticketoneTest")
			.withUsername("test")
			.withPassword("test");
	@Container
	@ServiceConnection
	static RedisContainer redis =
		new RedisContainer("redis:alpine")
			.withExposedPorts(6379)
			.waitingFor(Wait.forListeningPort());

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

	@Autowired
	private JwtConfig jwtConfig;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private User testUser;
	private String testEmail = "test@example.com";
	private String testPassword = "Test1234!";

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
			.role(UserRole.USER.getAuthority())
			.build();
		userRepository.save(testUser);

		// CustomUserDetails 생성
		CustomUserDetails userDetails = new CustomUserDetails(testUser);

		// 인증 토큰 생성 및 SecurityContext에 등록
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// Redis 블랙리스트 정리
		clearBlacklist();
	}

	@AfterAll
	void afterAll() {
		TestUtil.truncateAllTables(jdbcTemplate);
	}

	@Test
	@DisplayName("회원가입 성공")
	void 회원가입_성공() throws Exception {
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
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
			.contentType(MediaType.APPLICATION_JSON)
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
	@DisplayName("로그인 성공")
	void 로그인_성공() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.email(testEmail)
			.password(testPassword)
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest)));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("로그인 성공"));
	}

	@Test
	@DisplayName("로그아웃 성공")
	void 로그아웃_성공() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			post("/api/v1/users/logout")
				.header("Authorization", "Bearer " + tokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("로그아웃 성공"));
	}

	@Test
	@DisplayName("회원 탈퇴 성공")
	void 회원_탈퇴_성공() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			delete("/api/v1/users/me")
				.header("Authorization", "Bearer " + tokenInfo.getAccessToken())
				.cookie(refreshTokenCookie));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("회원 탈퇴 성공"));
	}

	@Test
	@DisplayName("중복된 이메일로 회원가입 시도")
	void 회원가입_중복_이메일() throws Exception {
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
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(signupRequest)));

		// then
		result.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("409-CONFLICT"))
			.andExpect(jsonPath("$.msg").value("이미 존재하는 이메일입니다."));
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인 시도")
	void 로그인_잘못된_비밀번호() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.email(testEmail)
			.password("WrongPassword123!")
			.build();

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest)));

		// then
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
			.andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요."));
	}

	@Test
	@DisplayName("프로필 조회 성공")
	void 프로필_조회_성공() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());

		// when
		ResultActions result = mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", "Bearer " + tokenInfo.getAccessToken())
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
	@DisplayName("프로필 수정 성공")
	void 프로필_수정_성공() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());
		UserUpdateRequest updateRequest = UserUpdateRequest.builder()
			.name("수정된이름")
			.phoneNum("010-9999-8888")
			.location("서울시 송파구")
			.build();

		// when
		ResultActions result = mockMvc.perform(
			put("/api/v1/users/me")
				.header("Authorization", "Bearer " + tokenInfo.getAccessToken())
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
	@DisplayName("잘못된 형식의 프로필 수정 요청")
	void 프로필_수정_잘못된_요청() throws Exception {
		// given
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		Cookie refreshTokenCookie = createTestRefreshTokenCookie(tokenInfo.getRefreshToken());
		UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
			.name("")
			.phoneNum("010-9999-8888")
			.location("서울시 송파구")
			.build();

		// when
		ResultActions result = mockMvc.perform(
			put("/api/v1/users/me")
				.header("Authorization", "Bearer " + tokenInfo.getAccessToken())
				.cookie(refreshTokenCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)));

		// then
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("데이터 형식이 잘못되었습니다."));
	}

	private Cookie createTestRefreshTokenCookie(String refreshToken) {
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		refreshTokenCookie.setHttpOnly(true);
		refreshTokenCookie.setSecure(false);  // 테스트 환경에서는 false로 설정
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
		return refreshTokenCookie;
	}

	private void clearBlacklist() {
		String testToken = jwtConfig.generateAccessToken(testUser.getEmail());
		String blacklistKey = "blacklist:" + testToken;
		redisTemplate.delete(blacklistKey);
	}
}