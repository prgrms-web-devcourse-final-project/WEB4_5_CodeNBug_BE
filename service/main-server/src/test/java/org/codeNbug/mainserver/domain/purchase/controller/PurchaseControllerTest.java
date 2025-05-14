package org.codeNbug.mainserver.domain.purchase.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;

import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.domain.seat.service.RedisKeyScanner;
import org.codeNbug.mainserver.global.Redis.entry.EntryTokenValidator;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PurchaseController.class)
@Import(PurchaseControllerTest.MockBeans.class)
class PurchaseControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PurchaseService purchaseService;

	@Autowired
	private EntryTokenValidator entryTokenValidator;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private RedisKeyScanner redisKeyScanner;

	@TestConfiguration
	static class MockBeans {
		@Bean
		public PurchaseService purchaseService() {
			return Mockito.mock(PurchaseService.class);
		}

		@Bean
		public StringRedisTemplate stringRedisTemplate() {
			StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
			HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
			when(redisTemplate.opsForHash()).thenReturn(hashOps);
			when(hashOps.get("ENTRY_TOKEN", "1")).thenReturn("testToken");
			return redisTemplate;
		}

		@Bean
		public RedisKeyScanner redisKeyScanner() {
			return Mockito.mock(RedisKeyScanner.class);
		}

		@Bean
		public EntryTokenValidator entryTokenValidator() {
			return Mockito.mock(EntryTokenValidator.class);
		}
	}

	@BeforeEach
	void setupSecurity() {
		User user = User.builder()
			.userId(1L)
			.email("test@codenbug.org")
			.password("encodedPw")
			.name("테스트유저")
			.sex("M")
			.phoneNum("01012345678")
			.location("서울시")
			.role("ROLE_USER")
			.age(25)
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication auth = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities()
		);

		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void tearDown() {
		if (redisTemplate.getConnectionFactory() != null) {
			redisTemplate.getConnectionFactory().getConnection().flushAll();
		}
	}

	@Test
	@DisplayName("결제 사전 등록 성공 - 200 반환")
	void initiatePayment_success() throws Exception {
		InitiatePaymentRequest request = new InitiatePaymentRequest(1L, 1000);
		InitiatePaymentResponse response = new InitiatePaymentResponse(1L, "IN_PROGRESS");

		given(purchaseService.initiatePayment(any(), anyLong())).willReturn(response);
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("결제 준비 완료"))
			.andExpect(jsonPath("$.data.purchaseId").value(1L))
			.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 사전 등록 실패 - 이벤트 없음 404 반환")
	void initiatePayment_fail_eventNotFound() throws Exception {
		Long invalidEventId = 999L;
		InitiatePaymentRequest request = new InitiatePaymentRequest(invalidEventId, 1000);

		// 실제 로직을 타지 않게
		given(purchaseService.initiatePayment(any(InitiatePaymentRequest.class), anyLong()))
			.willThrow(new IllegalArgumentException("행사가 존재하지 않습니다."));

		// when & then
		MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("행사가 존재하지 않습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 사전 등록 실패 - 선택된 좌석 정보 없음으로 인한 400 반환")
	void initiatePayment_fail_noSeatInfoInRedis() throws Exception {
		InitiatePaymentRequest request = new InitiatePaymentRequest(1L, 1000);

		given(redisKeyScanner.scanKeys("seat:lock:" + 1 + ":*:*"))
			.willReturn(Collections.emptySet());

		given(purchaseService.initiatePayment(any(InitiatePaymentRequest.class), anyLong()))
			.willThrow(new BadRequestException("[extractEventIdByUserId] 선택된 좌석 정보가 존재하지 않습니다."));

		MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("[extractEventIdByUserId] 선택된 좌석 정보가 존재하지 않습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 승인 성공 - 200 반환")
	void confirmPayment_success() throws Exception {
		ConfirmPaymentRequest request = new ConfirmPaymentRequest(1L, "paymentKey", "orderId", 1000);

		String url = "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20250502114628Tfml5&ref=PX";
		ConfirmPaymentResponse response = new ConfirmPaymentResponse(
			"paymentKey", "orderId", "지정석 1매", 1000, "DONE",
			PaymentMethodEnum.카드, LocalDateTime.now(), new ConfirmPaymentResponse.Receipt(url));

		given(purchaseService.confirmPayment(any(), anyLong())).willReturn(response);
		willDoNothing().given(entryTokenValidator).validate(anyLong(), anyString());

		MvcResult result = mockMvc.perform(post("/api/v1/payments/confirm")
				.header("entryAuthToken", "testToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("결제 승인 완료"))
			.andExpect(jsonPath("$.data.paymentKey").value("paymentKey"))
			.andExpect(jsonPath("$.data.orderId").value("orderId"))
			.andExpect(jsonPath("$.data.orderName").value("지정석 1매"))
			.andExpect(jsonPath("$.data.totalAmount").value(1000))
			.andExpect(jsonPath("$.data.status").value("DONE"))
			.andExpect(jsonPath("$.data.method").value("카드"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 승인 실패 - 구매 정보 없음 404 반환")
	void confirmPayment_fail_noPurchase() throws Exception {
		ConfirmPaymentRequest request = new ConfirmPaymentRequest(999L, "key", "orderId", 1000);

		given(purchaseService.confirmPayment(any(ConfirmPaymentRequest.class), anyLong()))
			.willThrow(new IllegalArgumentException("[confirm] 구매 정보를 찾을 수 없습니다."));

		MvcResult result = mockMvc.perform(post("/api/v1/payments/confirm")
				.header("entryAuthToken", "token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("[confirm] 구매 정보를 찾을 수 없습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 승인 실패 - 금액 불일치 400 반환")
	void confirmPayment_fail_amountMismatch() throws Exception {
		ConfirmPaymentRequest request = new ConfirmPaymentRequest(1L, "key", "orderId", 10000);

		given(purchaseService.confirmPayment(any(ConfirmPaymentRequest.class), anyLong()))
			.willThrow(new BadRequestException("[confirm] 결제 금액이 일치하지 않습니다."));

		MvcResult result = mockMvc.perform(post("/api/v1/payments/confirm")
				.header("entryAuthToken", "token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("[confirm] 결제 금액이 일치하지 않습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 승인 실패 - 일부 좌석 누락 400 반환")
	void confirmPayment_fail_missingSeats() throws Exception {
		ConfirmPaymentRequest request = new ConfirmPaymentRequest(1L, "key", "orderId", 1000);

		given(purchaseService.confirmPayment(any(ConfirmPaymentRequest.class), anyLong()))
			.willThrow(new BadRequestException("[confirm] 일부 좌석을 찾을 수 없습니다."));

		MvcResult result = mockMvc.perform(post("/api/v1/payments/confirm")
				.header("entryAuthToken", "token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400-BAD_REQUEST"))
			.andExpect(jsonPath("$.msg").value("[confirm] 일부 좌석을 찾을 수 없습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 취소 성공 - 200 반환")
	void cancelPayment_success() throws Exception {
		String paymentKey = "paymentKey";
		CancelPaymentRequest request = new CancelPaymentRequest("단순 변심");

		CancelPaymentResponse response = CancelPaymentResponse.builder()
			.status("CANCELED")
			.receiptUrl(
				"https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20250502114628Tfml5&ref=PX")
			.build();

		given(purchaseService.cancelPayment(any(), eq(paymentKey), anyLong())).willReturn(response);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/{paymentKey}/cancel", paymentKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.msg").value("결제 취소 완료"))
			.andExpect(jsonPath("$.data.status").value("CANCELED"))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("결제 취소 실패 - 결제 정보 없음 404 반환")
	void cancelPayment_fail_purchaseNotFound() throws Exception {
		String paymentKey = "paymentKey";
		CancelPaymentRequest request = new CancelPaymentRequest("단순 변심");

		given(purchaseService.cancelPayment(any(CancelPaymentRequest.class), eq(paymentKey), anyLong()))
			.willThrow(new IllegalArgumentException("[cancel] 해당 결제 정보를 찾을 수 없습니다."));

		MvcResult result = mockMvc.perform(post("/api/v1/payments/{paymentKey}/cancel", paymentKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404-NOT_FOUND"))
			.andExpect(jsonPath("$.msg").value("[cancel] 해당 결제 정보를 찾을 수 없습니다."))
			.andReturn();

		System.out.println(result.getResponse().getContentAsString());
	}
}