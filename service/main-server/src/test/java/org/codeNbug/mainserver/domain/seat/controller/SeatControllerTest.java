package org.codeNbug.mainserver.domain.seat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.util.BaseTestUtil;
import org.codeNbug.mainserver.util.TestUtil;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Transactional
class SeatControllerTest {
	@Autowired
	private BaseTestUtil baseTestUtil;

	@Container
	static MySQLContainer<?> mysql =
		new MySQLContainer<>("mysql:8.0.34")
			.withDatabaseName("ticketoneTest")
			.withUsername("test")
			.withPassword("test");
	@Container
	static GenericContainer<?> redis =
		new GenericContainer<>("redis:alpine")
			.withExposedPorts(6379)
			.waitingFor(Wait.forListeningPort());


	// 2) 스프링 프로퍼티에 컨테이너 URL/계정 주입
	@DynamicPropertySource
	static void overrideProps(DynamicPropertyRegistry registry) {

		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.redis.host", () -> redis.getHost());
		registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

	}
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SeatLayoutRepository seatLayoutRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private SeatGradeRepository seatGradeRepository;

	@Autowired
	private RedisLockService redisLockService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static User testUser;
	private static String testToken;
	private static Event testEvent;
	private static Seat availableSeat;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeAll
	public void setUpAll() throws JSONException {
		testUser = baseTestUtil.setUpUser();
		testToken = baseTestUtil.setUpToken();
		testEvent = baseTestUtil.setUpEvent();
		baseTestUtil.setUpRedis();
	}

	@AfterAll
	void tearDown() {
		TestUtil.truncateAllTables(jdbcTemplate);
	}

	@Test
	@Order(1)
	@DisplayName("좌석 조회 성공")
	void testGetSeatLayout() throws Exception {
		mockMvc.perform(get("/api/v1/event/{eventId}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("좌석 조회 성공"))
			.andExpect(jsonPath("$.data.seats[0].location").value("A1"))
			.andExpect(jsonPath("$.data.seats[0].grade").value("VIP"))
			.andExpect(jsonPath("$.data.seats[1].location").value("A2"))
			.andExpect(jsonPath("$.data.seats[1].grade").value("VIP"));
	}

	@Test
	@Order(2)
	@DisplayName("지정석 좌석 선택 - Redis 락 획득 확인")
	void selectSeat_withRedisLock() throws Exception {
		List<Seat> availableSeats = seatRepository.findFirstByEventIdAndAvailableTrue(testEvent.getEventId());
		availableSeat = availableSeats.get(0);

		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(availableSeat.getId()));
		request.setTicketCount(1);

		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/event/{eventId}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		String redisKey =
			"seat:lock:" + testUser.getUserId() + ":" + testEvent.getEventId() + ":" + request.getSeatList().getFirst();
		String lockValue = redisLockService.getLockValue(redisKey);
		assertThat(lockValue).isNotNull();
	}

	@Test
	@Order(3)
	@DisplayName("좌석 취소 성공 - Redis 락 해제 확인")
	void testCancelSeat() throws Exception {
		SeatCancelRequest request = new SeatCancelRequest();
		request.setSeatList(List.of(availableSeat.getId()));

		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(delete("/api/v1/event/{eventId}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("좌석 취소 성공"));

		String redisKey =
			"seat:lock:" + testUser.getUserId() + ":" + testEvent.getEventId() + ":" + request.getSeatList().getFirst();
		String lockValue = redisLockService.getLockValue(redisKey);
		assertThat(lockValue).isNull();
	}
}