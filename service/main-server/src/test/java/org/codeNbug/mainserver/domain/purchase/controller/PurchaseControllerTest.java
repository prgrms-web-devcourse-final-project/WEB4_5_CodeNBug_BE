package org.codeNbug.mainserver.domain.purchase.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseCancelRepository;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.codeNbug.mainserver.external.toss.service.TossPaymentServiceImpl;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PurchaseControllerTest {
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
	private TokenService tokenService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TossPaymentService tossPaymentService;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private PurchaseCancelRepository purchaseCancelRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private RedisLockService redisLockService;

	@Autowired
	private ManagerEventRepository managerEventRepository;

	@Autowired
	private PurchaseService purchaseService;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@InjectMocks
	private TossPaymentServiceImpl tossPaymentServiceImpl;

	private MockRestServiceServer mockRestServiceServer;

	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

	private static String testToken;
	private static User testUser;
	private static Event testEvent;
	private static Long purchaseId;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);

		tossPaymentServiceImpl = new TossPaymentServiceImpl(
			restTemplate,
			"test_sk_xxx",
			"https://api.tosspayments.com/v1/payments"
		);

		// PurchaseService 생성자 주입
		purchaseService = new PurchaseService(
			tossPaymentService,
			purchaseRepository,
			purchaseCancelRepository,
			userRepository,
			eventRepository,
			seatRepository,
			ticketRepository,
			redisLockService,
			managerEventRepository
		);
	}

	@BeforeAll
	public void setUpAll() throws JSONException {
		mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
		setUpUser();
		setUpEvent();
		setUpRedis();
	}

	private void setUpUser() {
		// 테스트용 사용자 생성
		testUser = User.builder()
			.email("test" + UUID.randomUUID() + "@example.com")
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
	}

	private void setUpEvent() throws JSONException {
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
		eventRepository.save(testEvent);

		// 테스트용 좌석 레이아웃 생성
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

		SeatLayout seatLayout = SeatLayout.builder()
			.layout(layoutJson)
			.event(testEvent)
			.build();
		seatLayoutRepository.save(seatLayout);

		testEvent.setSeatLayout(seatLayout);
		eventRepository.save(testEvent);

		// 좌석 등급 및 좌석 생성
		createSeatGradesAndSeats(layoutJson);
	}

	private void createSeatGradesAndSeats(String layoutJson) throws JSONException {
		JSONObject fullJson = new JSONObject(layoutJson);
		JSONArray rows = fullJson.getJSONArray("layout");
		JSONObject seatDetails = fullJson.getJSONObject("seat");

		Map<SeatGradeEnum, SeatGrade> gradeMap = new HashMap<>();
		for (SeatGradeEnum gradeEnum : SeatGradeEnum.values()) {
			SeatGrade seatGrade = SeatGrade.builder()
				.grade(gradeEnum)
				.amount(getSeatGradeAmount(gradeEnum))
				.event(testEvent)
				.build();
			seatGradeRepository.save(seatGrade);
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

				seatRepository.save(testSeat);
			}
		}
	}

	private int getSeatGradeAmount(SeatGradeEnum gradeEnum) {
		return switch (gradeEnum) {
			case VIP -> 100000;
			case R -> 80000;
			case S -> 60000;
			case A -> 50000;
			default -> 40000;
		};
	}

	private void setUpRedis() {
		// 테스트용 entry token 저장
		stringRedisTemplate.opsForHash().put(
			ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId()),
			"\"" + testToken + "\""
		);
	}

	@AfterAll
	void tearDown() {
		seatRepository.deleteAll();
		seatGradeRepository.deleteAll();
		ticketRepository.deleteAll();
		seatLayoutRepository.deleteAll();
		purchaseCancelRepository.deleteAll();
		purchaseRepository.deleteAll();
		eventRepository.deleteAll();
		userRepository.deleteAll();

		Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().flushAll();
	}

	@Test
	@Order(1)
	@Commit
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("결제 사전 등록 성공")
	void testInitiatePayment() throws Exception {
		List<Seat> availableSeats = seatRepository.findFirstByEventIdAndAvailableTrue(testEvent.getEventId());
		Seat seatToLock = availableSeats.get(0);

		SeatSelectRequest seatSelectRequest = new SeatSelectRequest();
		seatSelectRequest.setSeatList(List.of(seatToLock.getId()));
		seatSelectRequest.setTicketCount(1);

		String seatSelectJson = objectMapper.writeValueAsString(seatSelectRequest);

		mockMvc.perform(post("/api/v1/event/{eventId}/seats", testEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(seatSelectJson))
			.andExpect(status().isOk());

		String redisKey = "seat:lock:" + testUser.getUserId() + ":" + testEvent.getEventId() + ":" + seatToLock.getId();

		String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
		assertThat(redisValue).isNotNull();

		InitiatePaymentRequest paymentRequest = new InitiatePaymentRequest(testEvent.getEventId(), 1000);
		String paymentJson = objectMapper.writeValueAsString(paymentRequest);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(paymentJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.purchaseId").isNumber())
			.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		JsonNode jsonNode = objectMapper.readTree(responseBody);
		purchaseId = jsonNode.path("data").path("purchaseId").asLong();
	}

	@Test
	@Order(2)
	@DisplayName("등록된 결제 정보 확인")
	void testCheckPersistedPurchase() {
		Optional<Purchase> optionalPurchase = purchaseRepository.findById(purchaseId);
		assertThat(optionalPurchase).isPresent();
	}

	@Test
	@Order(3)
	@Commit
	@DisplayName("결제 승인 성공")
	void testConfirmPayment() throws Exception {
		ConfirmPaymentRequest paymentRequest = new ConfirmPaymentRequest(purchaseId, "paymentKey", "orderId", 1000);
		String tossResponseJson = """
			{
			    "paymentKey": "paymentKey",
			    "orderId": "orderId",
			    "orderName": "지정석 1매",
			    "totalAmount": 1000,
			    "status": "DONE",
			    "method": "카드",
			    "approvedAt": "2025-05-12T15:39:53Z",
			    "receipt": {
			        "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=paymentKey&ref=PX"
			    }
			}
			""";

		mockRestServiceServer.expect(
				MockRestRequestMatchers.requestTo("https://api.tosspayments.com/v1/payments/confirm"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(tossResponseJson, MediaType.APPLICATION_JSON));

		String paymentJson = objectMapper.writeValueAsString(paymentRequest);

		mockMvc.perform(post("/api/v1/payments/confirm")
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(paymentJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.orderId").value("orderId"))
			.andExpect(jsonPath("$.data.paymentKey").value("paymentKey"));

		mockRestServiceServer.verify();
	}

	@Test
	@Order(4)
	@Commit
	@DisplayName("Toss Webhook 수신 - 결제 완료 상태 처리")
	void testTossWebhookDone() throws Exception {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalStateException("테스트용 purchase 없음"));

		assertEquals(PaymentStatusEnum.IN_PROGRESS, purchase.getPaymentStatus());

		String payload = """
			{
			  "data": {
			    "paymentKey": "paymentKey",
			    "status": "DONE"
			  }
			}
			""";

		mockMvc.perform(post("/webhook/toss/status")
				.content(payload)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		Purchase updated = purchaseRepository.findById(purchaseId).orElseThrow();
		assertEquals(PaymentStatusEnum.DONE, updated.getPaymentStatus());
	}

	@Test
	@Order(5)
	@Commit
	@DisplayName("사용자 측 결제 취소 성공")
	void testCancelPayment() throws Exception {
		String paymentKey = "paymentKey";
		CancelPaymentRequest cancelRequest = new CancelPaymentRequest("단순변심");

		String tossResponseJson = """
			{
			    "paymentKey": "paymentKey",
			    "orderId": "orderId",
			    "status": "CANCELED",
			    "method": "카드",
			    "totalAmount": 1000,
			    "receipt": {
			        "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=paymentKey&ref=PX"
			    },
			    "cancels": [
			      {
			        "cancelAmount": 1000,
			        "canceledAt": "2025-05-12T15:40:00Z",
			        "cancelReason": "단순변심"
			      }
			    ]
			}
			""";

		mockRestServiceServer.expect(
				MockRestRequestMatchers.requestTo("https://api.tosspayments.com/v1/payments/paymentKey/cancel"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(tossResponseJson, MediaType.APPLICATION_JSON));

		String cancelRequestJson = objectMapper.writeValueAsString(cancelRequest);

		mockMvc.perform(post("/api/v1/payments/{paymentKey}/cancel", paymentKey)
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cancelRequestJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("결제 취소 완료"))
			.andExpect(jsonPath("$.data.paymentKey").value(paymentKey));

		mockRestServiceServer.verify();
	}

	@Test
	@Order(6)
	@Commit
	@DisplayName("Toss Webhook 수신 - 결제 취소 상태 처리")
	void testTossWebhookCanceled() throws Exception {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalStateException("테스트용 purchase 없음"));

		assertEquals(PaymentStatusEnum.DONE, purchase.getPaymentStatus());

		String payload = """
			{
			  "data": {
			    "paymentKey": "paymentKey",
			    "status": "CANCELED"
			  }
			}
			""";

		mockMvc.perform(post("/webhook/toss/status")
				.content(payload)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		Purchase updated = purchaseRepository.findById(purchaseId).orElseThrow();
		assertEquals(PaymentStatusEnum.CANCELED, updated.getPaymentStatus());
	}
}