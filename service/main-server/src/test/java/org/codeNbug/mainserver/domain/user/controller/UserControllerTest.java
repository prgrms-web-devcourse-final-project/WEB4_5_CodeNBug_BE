package org.codeNbug.mainserver.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.domain.user.service.UserService;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.common.util.JwtConfig;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SuppressWarnings({"deprecation"})
@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserService userService;

	@MockBean
	private TokenService tokenService;

	@MockBean
	private CookieUtil cookieUtil;

	@MockBean
	private JwtConfig jwtConfig;

	@MockBean
	private PurchaseService purchaseService;

	// Mock user data
	private User testUser;
	private String testEmail = "test@example.com";
	private String testPassword = "Test1234!";
	private TokenService.TokenInfo mockTokenInfo;

	@BeforeEach
	void setUp() {
		// Create test user
		testUser = User.builder()
				.email(testEmail)
				.password("encodedPassword")
				.name("테스트")
				.age(25)
				.sex("남성")
				.phoneNum("010-1234-5678")
				.location("서울시 강남구")
				.role(UserRole.USER.getAuthority())
				.build();

		// Mock token info
		mockTokenInfo = new TokenService.TokenInfo("test-access-token", "test-refresh-token");
	}

	@Test
	@WithMockUser    @DisplayName("회원가입 성공")
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

		SignupResponse signupResponse = SignupResponse.builder()
				.email("new@example.com")
				.name("신규사용자")
				.age(30)
				.sex("여성")
				.phoneNum("010-9876-5432")
				.location("서울시 서초구")
				.build();

		when(userService.signup(any(SignupRequest.class))).thenReturn(signupResponse);

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupRequest))
				.with(SecurityMockMvcRequestPostProcessors.csrf()));

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
	@WithMockUser    @DisplayName("로그인 성공")
	void 로그인_성공() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
				.email(testEmail)
				.password(testPassword)
				.build();

		LoginResponse loginResponse = LoginResponse.builder()
				.accessToken("test-access-token")
				.refreshToken("test-refresh-token")
				.tokenType("Bearer")
				.build();

		when(userService.login(any(LoginRequest.class))).thenReturn(loginResponse);
		doNothing().when(cookieUtil).setAccessTokenCookie(any(), anyString());
		doNothing().when(cookieUtil).setRefreshTokenCookie(any(), anyString());

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))
				.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200-SUCCESS"))
				.andExpect(jsonPath("$.msg").value("로그인 성공"));
	}

	@Test
	@WithMockUser    @DisplayName("로그아웃 성공")
	void 로그아웃_성공() throws Exception {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", "test-refresh-token");

		when(cookieUtil.getAccessTokenFromCookie(any())).thenReturn("test-access-token");
		when(cookieUtil.getRefreshTokenFromCookie(any())).thenReturn("test-refresh-token");
		doNothing().when(userService).logout(anyString(), anyString());
		doNothing().when(cookieUtil).expireAuthCookies(any(), any());

		// when
		ResultActions result = mockMvc.perform(
				post("/api/v1/users/logout")
						.header("Authorization", "Bearer test-access-token")
						.cookie(refreshTokenCookie)
						.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200-SUCCESS"))
				.andExpect(jsonPath("$.msg").value("로그아웃 성공"));
	}

	@Test
	@WithMockUser    @DisplayName("회원 탈퇴 성공")
	void 회원_탈퇴_성공() throws Exception {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", "test-refresh-token");

		when(cookieUtil.getAccessTokenFromCookie(any())).thenReturn("test-access-token");
		when(cookieUtil.getRefreshTokenFromCookie(any())).thenReturn("test-refresh-token");
		when(tokenService.getSubjectFromToken(anyString())).thenReturn(testEmail);
		doNothing().when(userService).withdrawUser(anyString(), anyString(), anyString());
		doNothing().when(cookieUtil).expireAuthCookies(any(), any());

		// when
		ResultActions result = mockMvc.perform(
				delete("/api/v1/users/me")
						.header("Authorization", "Bearer test-access-token")
						.cookie(refreshTokenCookie)
						.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200-SUCCESS"))
				.andExpect(jsonPath("$.msg").value("회원 탈퇴 성공"));
	}

	@Test
	@WithMockUser    @DisplayName("중복된 이메일로 회원가입 시도")
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

		when(userService.signup(any(SignupRequest.class))).thenThrow(new DuplicateEmailException("이미 존재하는 이메일입니다."));

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupRequest))
				.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("409-CONFLICT"))
				.andExpect(jsonPath("$.msg").value("이미 존재하는 이메일입니다."));
	}

	@Test
	@WithMockUser    @DisplayName("잘못된 비밀번호로 로그인 시도")
	void 로그인_잘못된_비밀번호() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
				.email(testEmail)
				.password("WrongPassword123!")
				.build();

		when(userService.login(any(LoginRequest.class))).thenThrow(new AuthenticationFailedException("비밀번호가 일치하지 않습니다. 남은 시도 횟수: 4회"));

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/users/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))
				.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("401-UNAUTHORIZED"))
				.andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다. 남은 시도 횟수: 4회"));
	}

	@Test
	@WithMockUser    @DisplayName("프로필 조회 성공")
	void 프로필_조회_성공() throws Exception {
		// given
		UserProfileResponse profileResponse = UserProfileResponse.builder()
				.id(1L)
				.email(testEmail)
				.name("테스트")
				.age(25)
				.sex("남성")
				.phoneNum("010-1234-5678")
				.location("서울시 강남구")
				.isSnsUser(false)
				.build();

		when(userService.getProfile()).thenReturn(profileResponse);

		// when
		ResultActions result = mockMvc.perform(
				get("/api/v1/users/me")
						.header("Authorization", "Bearer test-access-token")
						.with(SecurityMockMvcRequestPostProcessors.csrf()));

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
	@WithMockUser    @DisplayName("프로필 수정 성공")
	void 프로필_수정_성공() throws Exception {
		// given
		UserUpdateRequest updateRequest = UserUpdateRequest.builder()
				.name("수정된이름")
				.phoneNum("010-9999-8888")
				.location("서울시 송파구")
				.build();

		UserProfileResponse updatedProfile = UserProfileResponse.builder()
				.id(1L)
				.email(testEmail)
				.name("수정된이름")
				.age(25)
				.sex("남성")
				.phoneNum("010-9999-8888")
				.location("서울시 송파구")
				.isSnsUser(false)
				.build();

		when(userService.updateProfile(any(UserUpdateRequest.class))).thenReturn(updatedProfile);

		// when
		ResultActions result = mockMvc.perform(
				put("/api/v1/users/me")
						.header("Authorization", "Bearer test-access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updateRequest))
						.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200-SUCCESS"))
				.andExpect(jsonPath("$.msg").value("프로필 수정 성공"))
				.andExpect(jsonPath("$.data.name").value("수정된이름"))
				.andExpect(jsonPath("$.data.phoneNum").value("010-9999-8888"))
				.andExpect(jsonPath("$.data.location").value("서울시 송파구"));
	}

	@Test
	@WithMockUser    @DisplayName("잘못된 형식의 프로필 수정 요청")
	void 프로필_수정_잘못된_요청() throws Exception {
		// given
		UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
				.name("")
				.phoneNum("010-9999-8888")
				.location("서울시 송파구")
				.build();

		// when
		ResultActions result = mockMvc.perform(
				put("/api/v1/users/me")
						.header("Authorization", "Bearer test-access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest))
						.with(SecurityMockMvcRequestPostProcessors.csrf()));

		// then
		result.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
				.andExpect(jsonPath("$.msg").value("데이터 형식이 잘못되었습니다."));
	}
}