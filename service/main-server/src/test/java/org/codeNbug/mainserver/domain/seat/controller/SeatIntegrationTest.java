package org.codeNbug.mainserver.domain.seat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectResponse;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.Redis.entry.EntryTokenValidator;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Disabled
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SeatIntegrationTest {
	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.34")
		.withDatabaseName("ticketoneTest")
		.withUsername("test")
		.withPassword("test");

	@Container
	@ServiceConnection
	static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
		.withExposedPorts(6379)
		.waitingFor(Wait.forListeningPort())
		.withStartupTimeout(Duration.ofSeconds(30));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private SeatLayoutRepository seatLayoutRepository;

	@Autowired
	private SeatGradeRepository seatGradeRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private RedisLockService redisLockService;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private SeatService seatService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private ObjectMapper objectMapper;

	private Event testEvent;
	private User testUser;
	private String testToken;
	private SeatLayout seatLayout;
	private Seat seat1;
	private Seat seat2;

	@BeforeEach
	void setup() throws JSONException {
		testUser = testUser.builder()
			.email("test" + UUID.randomUUID() + "@example.com")
			.password("Test1234!")
			.name("테스트")
			.age(25)
			.sex("남성")
			.phoneNum("010-1234-5678")
			.location("서울시 강남구")
			.role(UserRole.USER.getAuthority())
			.build();
		userRepository.save(testUser);

		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		testToken = tokenInfo.getAccessToken();

		EventInformation info = EventInformation.builder()
			.title("테스트 공연")
			.thumbnailUrl("https://example.com/thumbnail.jpg")
			.description("이것은 테스트 공연입니다.")
			.ageLimit(15)
			.restrictions("음식물 반입 금지")
			.location("서울 강남구 공연장")
			.hallName("테스트홀")
			.eventStart(LocalDateTime.now().plusDays(1))
			.eventEnd(LocalDateTime.now().plusDays(2))
			.seatCount(200)
			.build();

		testEvent = new Event(
			EventCategoryEnum.CONCERT,
			info,
			LocalDateTime.now().plusDays(1),
			LocalDateTime.now().plusDays(2),
			0,
			LocalDateTime.now(),
			LocalDateTime.now(),
			EventStatusEnum.OPEN,
			true,
			false,
			null
		);
		testEvent = eventRepository.save(testEvent);

		String layoutJson = """
			{
			  "layout": [
			    ["A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10"],
			    ["B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "B10"]
			  ],
			  "seat": {
			    "A1": { "grade": "VIP" },
			    "A2": { "grade": "VIP" },
			    "A3": { "grade": "VIP" },
			    "A4": { "grade": "VIP" },
			    "A5": { "grade": "VIP" },
			    "A6": { "grade": "VIP" },
			    "A7": { "grade": "VIP" },
			    "A8": { "grade": "VIP" },
			    "A9": { "grade": "VIP" },
			    "A10": { "grade": "VIP" },
			    "B1": { "grade": "R" },
			    "B2": { "grade": "R" },
			    "B3": { "grade": "R" },
			    "B4": { "grade": "R" },
			    "B5": { "grade": "R" },
			    "B6": { "grade": "R" },
			    "B7": { "grade": "R" },
			    "B8": { "grade": "R" },
			    "B9": { "grade": "R" },
			    "B10": { "grade": "R" }
			  }
			}
			""";

		seatLayout = SeatLayout.builder()
			.layout(layoutJson)
			.event(testEvent)
			.build();
		seatLayout = seatLayoutRepository.save(seatLayout);
		testEvent.setSeatLayout(seatLayout);
		eventRepository.save(testEvent);

		JSONObject fullJson = new JSONObject(layoutJson);
		JSONArray rows = fullJson.getJSONArray("layout");
		JSONObject seatDetails = fullJson.getJSONObject("seat");

		Map<SeatGradeEnum, SeatGrade> gradeMap = new HashMap<>();
		for (SeatGradeEnum gradeEnum : SeatGradeEnum.values()) {
			SeatGrade seatGrade = SeatGrade.builder()
				.grade(gradeEnum)
				.amount(switch (gradeEnum) {
					case VIP -> 100000;
					case R -> 80000;
					case S -> 60000;
					case A -> 50000;
					default -> 40000;
				})
				.event(testEvent)
				.build();
			seatGrade = seatGradeRepository.save(seatGrade);
			gradeMap.put(gradeEnum, seatGrade);
		}

		// 좌석 생성
		for (int i = 0; i < rows.length(); i++) {
			JSONArray row = rows.optJSONArray(i);
			if (row == null)
				continue;

			for (int j = 0; j < row.length(); j++) {
				String seatName = row.getString(j);
				String grade = seatDetails.getJSONObject(seatName).getString("grade");

				SeatGradeEnum gradeEnum = SeatGradeEnum.fromString(grade);
				SeatGrade seatGrade = gradeMap.get(gradeEnum);

				Seat testSeat = Seat.builder()
					.location(seatName)
					.grade(seatGrade)
					.layout(testEvent.getSeatLayout())
					.event(testEvent)
					.available(true)
					.build();

				Seat saved = seatRepository.save(testSeat);
				if (seatName.equals("A1")) {
					seat1 = saved;
				} else if (seatName.equals("A2")) {
					seat2 = saved;
				}
			}
		}

		redisTemplate.opsForHash().put(EntryTokenValidator.ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId()),
			testToken);
	}

	@Test
	void testRedisLockServiceWorks() {
		String lockKey = "seat:lock:26:1:22";
		String lockValue = "lock-value";

		boolean locked = redisLockService.tryLock(lockKey, lockValue, Duration.ofSeconds(5));
		assertThat(locked).isTrue();

		String storedValue = redisLockService.getLockValue(lockKey);
		assertThat(storedValue).isEqualTo(lockValue);

		boolean unlocked = redisLockService.unlock(lockKey, lockValue);
		assertThat(unlocked).isTrue();

		String afterUnlockValue = redisLockService.getLockValue(lockKey);
		assertThat(afterUnlockValue).isNull();
	}

	@Test
	@DisplayName("좌석 조회 성공")
	void testGetSeatLayout_Success() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/event/{event-id}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("좌석 조회 성공"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("좌석 선택 성공")
	void testSelectSeat_Success() throws Exception {
		SeatSelectRequest request = new SeatSelectRequest();
		request.setSeatList(List.of(seat1.getId()));
		request.setTicketCount(1);

		String jsonRequest = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/event/{event-id}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.content(jsonRequest)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(result -> {
				String json = result.getResponse().getContentAsString();

				RsData<SeatSelectResponse> rsData = objectMapper.readValue(
					json,
					new TypeReference<RsData<SeatSelectResponse>>() {
					}
				);

				assertThat(rsData.getCode()).isEqualTo("200");
				assertThat(rsData.getMsg()).isEqualTo("좌석 선택 성공");

				SeatSelectResponse seatSelectResponse = rsData.getData();
				assertThat(seatSelectResponse).isNotNull();
				assertThat(seatSelectResponse.getSeatList()).contains(seat1.getId());
			});
	}

	@Test
	@DisplayName("좌석 취소 성공")
	void testCancelSeat_Success() throws Exception {
		seat1.reserve();
		seatRepository.save(seat1);

		SeatCancelRequest request = new SeatCancelRequest();
		request.setSeatList(List.of(seat1.getId()));

		String jsonRequest = objectMapper.writeValueAsString(request);

		mockMvc.perform(delete("/api/v1/event/{event-id}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.content(jsonRequest)
				.header("entryAuthToken", "valid-token")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(result -> {
				String json = result.getResponse().getContentAsString();

				RsData<Void> rsData = objectMapper.readValue(
					json,
					new TypeReference<RsData<Void>>() {
					}
				);

				assertThat(rsData.getCode()).isEqualTo("200");
				assertThat(rsData.getMsg()).isEqualTo("좌석 취소 성공");
			});

		Seat seat = seatRepository.findById(seat1.getId()).orElseThrow();
		assertThat(seat.isAvailable()).isTrue();
	}
}