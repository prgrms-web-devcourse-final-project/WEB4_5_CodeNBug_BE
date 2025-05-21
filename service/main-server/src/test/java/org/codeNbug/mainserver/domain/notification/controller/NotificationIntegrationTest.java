package org.codeNbug.mainserver.domain.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.domain.notification.dto.NotificationCreateRequestDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDeleteRequestDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.util.BaseTestUtil;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@ActiveProfiles("test")
@Testcontainers
class NotificationIntegrationTest {

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
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private BaseTestUtil baseTestUtil;

	@Autowired
	private CustomUserDetailsService userDetailsService;

	private User testUser;
	private User adminUser;
	private String testToken;
	private String adminToken;

	@BeforeAll
	void setUp() {
		// 일반 테스트 사용자 설정
		testUser = baseTestUtil.setUpUser();
		testToken = baseTestUtil.setUpToken();

		// 관리자 사용자 생성 (알림 생성 API 테스트 용)
		adminUser = User.builder()
			.email("admin@example.com")
			.password(passwordEncoder.encode("Admin1234!"))
			.name("관리자")
			.age(30)
			.sex("남성")
			.phoneNum("010-9876-5432")
			.location("서울시 강남구")
			.role(UserRole.ADMIN.getAuthority()) // "ROLE_ADMIN"
			.build();
		userRepository.save(adminUser);

		// 관리자용 토큰 생성
		TokenService.TokenInfo adminTokenInfo = tokenService.generateTokens(adminUser.getEmail());
		adminToken = adminTokenInfo.getAccessToken();

		// 테스트용 알림 데이터 생성
		createTestNotifications();
	}

	@AfterAll
	void tearDown() {
		// 테스트 후 데이터 정리
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	private void createTestNotifications() {
		// 여러 상태와 유형의 알림 데이터 생성
		notificationService.createNotification(testUser.getUserId(), NotificationEnum.SYSTEM, "시스템 알림입니다.");
		notificationService.createNotification(testUser.getUserId(), NotificationEnum.EVENT, "이벤트 알림입니다.");
		notificationService.createNotification(testUser.getUserId(), NotificationEnum.TICKET, "티켓 알림입니다.");
	}

	private void setAuthenticationForUser(User user) {
		// 사용자 인증 정보 설정 - CustomUserDetails 사용
		UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@BeforeEach
	void setUpAuth() {
		// 각 테스트 시작 전 테스트 사용자로 인증 설정
		setAuthenticationForUser(testUser);
	}

	@Test
	@DisplayName("알림 생성 호출 테스트")
	void createNotification() throws Exception {
		// 관리자 사용자로 인증 설정
		setAuthenticationForUser(adminUser);

		// 요청 DTO 생성
		NotificationCreateRequestDto requestDto = new NotificationCreateRequestDto(
			testUser.getUserId(),
			NotificationEnum.SYSTEM,
			"API 알림 제목",
			"API를 통해 생성된 테스트 알림입니다."
		);

		// API 호출
		ResultActions result = mockMvc.perform(post("/api/v1/notifications")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(requestDto)));

		// 응답 검증
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("알림 생성 성공"))
			.andExpect(jsonPath("$.data.type").value("SYSTEM"))
			.andExpect(jsonPath("$.data.content").value("API를 통해 생성된 테스트 알림입니다."))
			.andExpect(jsonPath("$.data.read").value(false));

		// DB에 실제로 생성됐는지 확인
		List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(testUser.getUserId());
		boolean found = notifications.stream()
			.anyMatch(n -> "API를 통해 생성된 테스트 알림입니다.".equals(n.getContent()));

		if (!found) {
			throw new AssertionError("알림이 데이터베이스에 생성되지 않았습니다.");
		}
	}

	@Test
	@DisplayName("알림 목록 조회 테스트")
	void getNotifications() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/v1/notifications")
			.header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("알림 목록 조회 성공"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3));
	}

	@Test
	@DisplayName("읽지 않은 알림 조회 테스트")
	void getUnreadNotifications() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/v1/notifications/unread")
			.header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("미읽은 알림 조회 성공"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(3)); // 모든 알림이 읽지 않은 상태
	}

	@Test
	@DisplayName("알림 상세 조회 테스트")
	void getNotificationDetail() throws Exception {
		// given
		Long notificationId = getFirstNotificationId();

		// when
		ResultActions result = mockMvc.perform(get("/api/v1/notifications/{id}", notificationId)
			.header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("알림 조회 성공"))
			.andExpect(jsonPath("$.data.id").value(notificationId))
			.andExpect(jsonPath("$.data.read").value(true)); // 조회 후 읽음 상태로 변경됨
	}

	@Test
	@DisplayName("단일 알림 삭제 테스트")
	void deleteNotification() throws Exception {
		// given
		Long notificationId = getFirstNotificationId();

		// when
		ResultActions result = mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId)
			.header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("알림 삭제 성공"));

		// 데이터베이스에서 실제로 삭제됐는지 확인
		Optional<Notification> deletedNotification = notificationRepository.findById(notificationId);
		if (deletedNotification.isPresent()) {
			throw new AssertionError("알림이 삭제되지 않았습니다: " + notificationId);
		}
	}

	@Test
	@DisplayName("다건 알림 삭제 테스트")
	void deleteNotifications() throws Exception {
		// given
		List<Long> notificationIds = getNotificationIds(2);
		NotificationDeleteRequestDto request = new NotificationDeleteRequestDto(notificationIds);

		// when
		ResultActions result = mockMvc.perform(post("/api/v1/notifications/batch-delete")
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("200-SUCCESS"))
				.andExpect(jsonPath("$.msg").value("알림 삭제 성공"));

		// 데이터베이스에서 실제로 삭제됐는지 확인
		for (Long id : notificationIds) {
			Optional<Notification> deletedNotification = notificationRepository.findById(id);
			if (deletedNotification.isPresent()) {
				throw new AssertionError("알림이 삭제되지 않았습니다: " + id);
			}
		}
	}

	@Test
	@DisplayName("모든 알림 삭제 테스트")
	void deleteAllNotifications() throws Exception {
		// when
		ResultActions result = mockMvc.perform(delete("/api/v1/notifications/all")
			.header("Authorization", "Bearer " + testToken));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200-SUCCESS"))
			.andExpect(jsonPath("$.msg").value("모든 알림 삭제 성공"));

		// 데이터베이스에서 실제로 모든 알림이 삭제됐는지 확인
		List<Notification> remainingNotifications = notificationRepository.findByUserIdOrderBySentAtDesc(
			testUser.getUserId());
		if (!remainingNotifications.isEmpty()) {
			throw new AssertionError("모든 알림이 삭제되지 않았습니다. 남은 알림 수: " + remainingNotifications.size());
		}
	}

	private Long getFirstNotificationId() {
		List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(testUser.getUserId());
		if (notifications.isEmpty()) {
			throw new IllegalStateException("테스트 알림이 존재하지 않습니다.");
		}
		return notifications.get(0).getId();
	}

	private List<Long> getNotificationIds(int count) {
		List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(testUser.getUserId());
		if (notifications.size() < count) {
			throw new IllegalStateException("요청한 개수만큼의 테스트 알림이 존재하지 않습니다.");
		}
		return notifications.subList(0, count).stream().map(Notification::getId).collect(Collectors.toList());
	}
}