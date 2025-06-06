package org.codeNbug.mainserver.domain.purchase.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
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
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.codeNbug.mainserver.external.toss.service.TossPaymentServiceImpl;
import org.codeNbug.mainserver.util.BaseTestUtil;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Testcontainers
@Commit
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PurchaseIntegrationTest {
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
	private BaseTestUtil baseTestUtil;

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
	private TossPaymentService tossPaymentService;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private PurchaseCancelRepository purchaseCancelRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private SeatService seatService;

	@Autowired
	private RedisLockService redisLockService;

	@Autowired
	private ManagerEventRepository managerEventRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PurchaseService purchaseService;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private TossPaymentServiceImpl tossPaymentServiceImpl;

	private MockRestServiceServer mockRestServiceServer;

	private User testUser;
	private String testToken;
	private Event SelecatbleTestEvent;
	private Long purchaseId;
	private String entryToken;
	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

	@BeforeEach
	void setUp() {
		tossPaymentServiceImpl = new TossPaymentServiceImpl(
			restTemplate,
			"test_sk_xxx",
			"https://api.tosspayments.com/v1/payments"
		);
	}

	@BeforeAll
	public void setUpAll() throws Exception {
		mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);

		redisTemplate.opsForValue().set("redisKey", "locked", Duration.ofMinutes(5));

		testUser = baseTestUtil.setUpUser();
		testToken = baseTestUtil.setUpToken();
		SelecatbleTestEvent = baseTestUtil.setUpSelecatbleEvent();

		String url = UriComponentsBuilder.fromHttpUrl("http://localhost:9001/api/v1/events/{eventId}/tickets/waiting")
			.buildAndExpand(SelecatbleTestEvent.getEventId())
			.toUriString();

		mockRestServiceServer.expect(
				requestTo(url))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("{\"data\": {\"entryAuthToken\": \"testToken\"}}", MediaType.APPLICATION_JSON));

		redisTemplate.opsForHash().put(
			ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId()),
			testToken
		);

		entryToken = "testToken";
	}

	// @AfterAll
	// void tearDown() {
	// 	seatRepository.deleteAll();
	// 	seatGradeRepository.deleteAll();
	// 	ticketRepository.deleteAll();
	// 	seatLayoutRepository.deleteAll();
	// 	purchaseCancelRepository.deleteAll();
	// 	purchaseRepository.deleteAll();
	// 	eventRepository.deleteAll();
	// 	userRepository.deleteAll();
	// }

	@Test
	@Order(1)
	@Commit
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("결제 사전 등록 성공")
	void testInitiatePayment() throws Exception {
		List<Seat> availableSeats = seatRepository.findFirstByEventIdAndAvailableTrue(SelecatbleTestEvent.getEventId());
		Seat seatToLock = availableSeats.get(0);

		SeatSelectRequest seatSelectRequest = new SeatSelectRequest();
		seatSelectRequest.setSeatList(List.of(seatToLock.getId()));
		seatSelectRequest.setTicketCount(1);

		String seatSelectJson = objectMapper.writeValueAsString(seatSelectRequest);
		System.out.println(seatSelectJson);

		System.out.println("Service is null? " + (seatService == null));

		mockMvc.perform(post("/api/v1/event/{eventId}/seats", SelecatbleTestEvent.getEventId())
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", entryToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(seatSelectJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("좌석 선택 성공"))
			.andDo(print());

		System.out.println("token:: " + testToken);
		String redisKey =
			"seat:lock:" + testUser.getUserId() + ":" + SelecatbleTestEvent.getEventId() + ":" + seatToLock.getId();
		String redisValue = redisTemplate.opsForValue().get(redisKey);
		System.out.println("Redis 조회 결과: " + redisValue); // null이면 저장 안 된 것
		assertThat(redisValue).isNotNull();

		InitiatePaymentRequest paymentRequest = new InitiatePaymentRequest(SelecatbleTestEvent.getEventId(), 1000);
		String paymentJson = objectMapper.writeValueAsString(paymentRequest);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
				.header("Authorization", "Bearer " + testToken)
				.header("entryAuthToken", entryToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(paymentJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		JsonNode jsonNode = objectMapper.readTree(responseBody);
		purchaseId = jsonNode.path("data").path("purchaseId").asLong();
		System.out.println("purchaseId:: " + purchaseId);
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
				.header("entryAuthToken", entryToken)
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
			.andExpect(jsonPath("$.msg").value("결제 취소 완료"));

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
