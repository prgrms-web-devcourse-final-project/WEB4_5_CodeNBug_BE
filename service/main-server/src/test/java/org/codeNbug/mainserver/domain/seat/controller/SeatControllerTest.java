package org.codeNbug.mainserver.domain.seat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeatControllerTest {
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
	private TokenService tokenService;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StringRedisTemplate redisTemplate;

	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

	private static String testToken;
	private static User testUser;
	private static Event testEvent;
	private static Seat availableSeat;

	@BeforeAll
	public void setUp() throws JSONException {
		// 테스트용 사용자 생성
		testUser = User.builder()
			.email("tdfs23231dfs12311111@example.com")
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
		UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		testToken = tokenInfo.getAccessToken();

		// 테스트용 행사 생성
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
			1L,
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
		eventRepository.save(testEvent);

		// 테스트용 좌석 레이아웃 생성
		String layoutJson = """
			{
			  "layout": [
				["A1", "A2"],
			  ],
			  "seat": {
			  	"A1": { "grade": "VIP" }, 
			  	"A2": { "grade": "R" }
			  	}
			 }
			""";

		SeatLayout seatLayout = SeatLayout.builder()
			.layout(layoutJson)
			.event(testEvent)
			.build();
		seatLayoutRepository.save(seatLayout);

		testEvent.setSeatLayout(seatLayout);
		eventRepository.save(testEvent);

		// 테스트용 좌석 등급 생성
		JSONArray rows = new JSONObject(layoutJson).getJSONArray("layout");
		JSONObject seatDetails = new JSONObject(layoutJson).getJSONObject("seat");

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

			seatGradeRepository.save(seatGrade);
			gradeMap.put(gradeEnum, seatGrade);
		}

		// 테스트용 좌석 생성
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
					.layout(seatLayout)
					.event(testEvent)
					.available(true)
					.build();

				seatRepository.save(testSeat);
			}
		}
	}

	@BeforeEach
	void setUpRedis() {
		// 테스트용 entry token 저장
		redisTemplate.opsForHash().put(
			ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId()),
			"\"" + testToken + "\""
		);
	}

	@AfterAll
	void tearDown() {
		userRepository.deleteAll();
		seatRepository.deleteAll();
		seatLayoutRepository.deleteAll();
		seatGradeRepository.deleteAll();
		eventRepository.deleteAll();
		redisTemplate.opsForHash().delete(
			ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId())
		);
	}

	@Test
	@Commit
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
			.andExpect(jsonPath("$.data.seats[1].grade").value("R"));
	}

	@Test
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