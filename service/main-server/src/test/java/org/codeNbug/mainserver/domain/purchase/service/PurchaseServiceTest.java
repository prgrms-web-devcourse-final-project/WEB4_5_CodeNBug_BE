package org.codeNbug.mainserver.domain.purchase.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

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
	private RedisLockService redisLockService;

	private Long userId;
	private Long eventId;
	private User user;
	private Event event;
	private Purchase purchase;
	private SeatLayout seatLayout;
	private Seat seat1, seat2;
	private Ticket ticket;

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
	}

	@Test
	@DisplayName("결제 사전 등록 성공")
	void initiatePayment_success() {
		// given
		InitiatePaymentRequest request = new InitiatePaymentRequest(eventId, 10000);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(redisLockService.extractEventIdByUserId(userId)).willReturn(eventId);
		given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
		given(purchaseRepository.save(any(Purchase.class))).willReturn(purchase);

		// when
		InitiatePaymentResponse response = purchaseService.initiatePayment(request, userId);

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

		InitiatePaymentRequest request = new InitiatePaymentRequest(eventId, 10000);

		// when & then
		assertThatThrownBy(() -> purchaseService.initiatePayment(request, userId))
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

		InitiatePaymentRequest request = new InitiatePaymentRequest(eventId, 10000);

		// when & then
		assertThatThrownBy(() -> purchaseService.initiatePayment(request, userId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("[init] 행사가 존재하지 않습니다.");
	}
}