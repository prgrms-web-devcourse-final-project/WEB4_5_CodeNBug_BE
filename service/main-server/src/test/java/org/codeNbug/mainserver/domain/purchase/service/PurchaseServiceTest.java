package org.codeNbug.mainserver.domain.purchase.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseCancelRepository;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.dto.CanceledPaymentInfo;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PurchaseServiceTest {

	@InjectMocks
	private PurchaseService purchaseService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private PurchaseRepository purchaseRepository;

	@Mock
	private PurchaseCancelRepository purchaseCancelRepository;

	@Mock
	private RedisLockService redisLockService;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private TossPaymentService tossPaymentService;

	@Mock
	private NotificationService notificationService;

	private Long userId;
	private Long eventId;
	private User user;
	private Event event;
	private Purchase purchase;
	private SeatLayout seatLayout;
	private Seat seat1, seat2;
	private Ticket ticket;

	private InitiatePaymentRequest initiateRequest;
	private ConfirmPaymentRequest confirmRequest;
	private ConfirmPaymentRequest confirmZeroAmountRequest;
	private CancelPaymentRequest cancelRequest;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userId = 1L;
		eventId = 100L;

		event = new Event();
		ReflectionTestUtils.setField(event, "eventId", eventId);
		ReflectionTestUtils.setField(event, "seatSelectable", true); // 이걸로 예매 모드 설정

		seatLayout = SeatLayout.builder()
			.id(10L)
			.event(event)
			.layout("{\"layout\":[[\"A1\",\"A2\"]],\"seat\":{\"A1\":{\"grade\":\"VIP\"},\"A2\":{\"grade\":\"R\"}}}")
			.build();

		SeatGrade vipGrade = SeatGrade.builder()
			.id(1L)
			.grade(SeatGradeEnum.VIP)
			.amount(100000)
			.event(event)
			.build();

		SeatGrade rGrade = SeatGrade.builder()
			.id(2L)
			.grade(SeatGradeEnum.R)
			.amount(80000)
			.event(event)
			.build();

		seat1 = Seat.builder()
			.id(1L)
			.location("A1")
			.grade(vipGrade)
			.available(true)
			.layout(seatLayout)
			.event(event)
			.build();

		seat2 = Seat.builder()
			.id(2L)
			.location("A2")
			.grade(rGrade)
			.available(true)
			.layout(seatLayout)
			.event(event)
			.build();

		user = User.builder().userId(userId).email("test@codenbug.org").build();

		purchase = Purchase.builder()
			.id(1L)
			.user(user)
			.amount(10000)
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.build();

		ticket = new Ticket(1L, "A1", LocalDateTime.now(), event, purchase);

		initiateRequest = new InitiatePaymentRequest(eventId, 10000);
		confirmRequest = new ConfirmPaymentRequest(1L, "paymentKey", "orderId", 10000);
		confirmZeroAmountRequest = new ConfirmPaymentRequest(1L, "paymentKey", "orderId", 0);
		cancelRequest = new CancelPaymentRequest("단순 변심");
	}

	@Test
	@DisplayName("결제 사전 등록 성공")
	void initiatePayment_success() {
		// given
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(purchaseRepository.save(any(Purchase.class))).willReturn(purchase);

		// when
		InitiatePaymentResponse response = purchaseService.initiatePayment(initiateRequest, userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
	}

	@Test
	@DisplayName("결제 사전 등록 실패 - 이벤트 ID 추출 실패")
	void initiatePayment_fail_noSeatInfoInRedis() {
		// given
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(redisLockService.extractEventIdByUserId(userId))
			.willThrow(new IllegalStateException("[extractEventIdByUserId] 선택된 좌석 정보가 존재하지 않습니다."));

		// when & then
		assertThatThrownBy(() -> purchaseService.initiatePayment(initiateRequest, userId))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("[extractEventIdByUserId] 선택된 좌석 정보가 존재하지 않습니다.");
	}

	@Test
	@DisplayName("결제 사전 등록 실패 - 이벤트 없음")
	void initiatePayment_fail_eventNotFound() {
		// given
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(eventRepository.findById(eventId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> purchaseService.initiatePayment(initiateRequest, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("[init] 행사가 존재하지 않습니다.");
	}

	@Test
	@DisplayName("결제 승인 성공")
	void confirmPayment_success() throws Exception {
		// Given
		ConfirmedPaymentInfo info = new ConfirmedPaymentInfo(
			"paymentKey", "orderId", "지정석 2매", 10000, "DONE",
			"카드", OffsetDateTime.now().toString(), new ConfirmedPaymentInfo.Receipt("receiptUrl")
		);

		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(redisLockService.getLockedSeatIdsByUserId(userId)).willReturn(List.of(1L, 2L));
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(seatRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(seat1, seat2));
		given(tossPaymentService.confirmPayment("paymentKey", "orderId", 10000)).willReturn(info);

		// When
		ConfirmPaymentResponse response = purchaseService.confirmPayment(confirmRequest, userId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("DONE");
		assertThat(response.getMethod().name()).isEqualTo("카드");
		assertThat(response.getOrderId()).isEqualTo("orderId");
	}

	@Test
	@DisplayName("결제 승인 실패 - 구매 정보 없음")
	void confirmPayment_fail_purchaseNotFound() {
		// Given
		given(purchaseRepository.findById(1L)).willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> purchaseService.confirmPayment(confirmRequest, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("구매 정보를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("결제 승인 실패 - 금액 불일치")
	void confirmPayment_fail_amountMismatch() {
		// Given
		ReflectionTestUtils.setField(purchase, "amount", 5000); // 요청과 다르게
		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

		// When & Then
		assertThatThrownBy(() -> purchaseService.confirmPayment(confirmRequest, userId))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("[confirm] 결제 금액이 일치하지 않습니다");
	}

	@Test
	@DisplayName("결제 승인 실패 - 이벤트 없음")
	void confirmPayment_fail_eventNotFound() {
		// Given
		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(redisLockService.getLockedSeatIdsByUserId(userId)).willReturn(List.of(1L, 2L));
		given(eventRepository.findById(eventId)).willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> purchaseService.confirmPayment(confirmRequest, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("[confirm] 이벤트 정보를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("결제 승인 실패 - 일부 좌석 없음")
	void confirmPayment_fail_missingSeats() {
		// Given
		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(redisLockService.getLockedSeatIdsByUserId(userId)).willReturn(List.of(1L, 2L));
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(seatRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(seat1)); // 1개만 반환

		// When & Then
		assertThatThrownBy(() -> purchaseService.confirmPayment(confirmRequest, userId))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("[confirm] 일부 좌석을 찾을 수 없습니다");
	}

	@Test
	@DisplayName("결제 승인 실패 - 결제 금액 0원")
	void confirmPayment_fail_amountZero() {
		// given
		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

		// when & then
		assertThatThrownBy(() -> purchaseService.confirmPayment(confirmZeroAmountRequest, userId))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("[confirm] 결제 금액이 0원입니다.");
	}

	@DisplayName("결제 실패 - redis Lock 해제")
	@Test
	void confirmPayment_fail() throws IOException, InterruptedException {
		// given
		given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(redisLockService.getLockedSeatIdsByUserId(userId)).willReturn(List.of(1L, 2L));
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(seatRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(seat1, seat2));

		// toss 결제 실패 유도
		given(tossPaymentService.confirmPayment(any(), any(), anyInt()))
			.willThrow(new IOException("결제 API 실패"));

		// when
		assertThrows(IOException.class, () -> {
			purchaseService.confirmPayment(confirmRequest, userId);
		});

		// then
		verify(redisLockService).releaseAllLocks(userId);
		verify(redisLockService).releaseAllEntryQueueLocks(userId);
	}

	@DisplayName("결제 취소 성공")
	@Test
	void cancelPayment_success() {
		// given
		ReflectionTestUtils.setField(purchase, "paymentUuid", "paymentKey");
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(purchaseRepository.findByPaymentUuid("paymentKey")).willReturn(Optional.of(purchase));

		CanceledPaymentInfo.CancelDetail detail = new CanceledPaymentInfo.CancelDetail(
			1000, "2025-05-14T12:00:00Z", "단순 변심"
		);
		CanceledPaymentInfo info = new CanceledPaymentInfo(
			"paymentKey", "orderId", "CANCELED", "카드", 1000, null,
			List.of(detail)
		);

		given(tossPaymentService.cancelPayment("paymentKey", "단순 변심")).willReturn(info);
		given(ticketRepository.findAllByPurchaseId(purchase.getId())).willReturn(List.of(ticket));
		given(seatRepository.findByTicketId(ticket.getId())).willReturn(List.of(seat1));

		// when
		CancelPaymentResponse response = purchaseService.cancelPayment(cancelRequest, "paymentKey", userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("CANCELED");
		assertThat(response.getCancels().get(0).getCancelAmount()).isEqualTo(1000);
		assertThat(response.getCancels().get(0).getCancelReason()).isEqualTo("단순 변심");
	}

	@DisplayName("결제 취소 실패 - 결제 정보 없음")
	@Test
	void cancelPayment_fail_purchaseNotFound() {
		// given
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(purchaseRepository.findByPaymentUuid("invalidKey")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> purchaseService.cancelPayment(cancelRequest, "invalidKey", userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("[cancel] 해당 결제 정보를 찾을 수 없습니다.");
	}
}