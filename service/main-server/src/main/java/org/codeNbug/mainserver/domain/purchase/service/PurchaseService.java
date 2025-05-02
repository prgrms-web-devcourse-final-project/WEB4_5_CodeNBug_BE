package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisKeyScanner;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {
	private final TossPaymentService tossPaymentService;
	private final PurchaseRepository purchaseRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;
	private final TicketRepository ticketRepository;
	private final RedisKeyScanner redisKeyScanner;
	private final RedisTemplate<String, String> redisTemplate;
	private final RedisLockService redisLockService;

	/**
	 * 결제 사전 등록 처리
	 * - 결제 UUID를 생성하고 결제 상태를 '진행 중'으로 설정하여 저장
	 *
	 * @param request 이벤트 ID 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		Long eventId = redisLockService.extractEventIdByUserId(userId);

		eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("행사가 존재하지 않습니다."));

		Purchase purchase = Purchase.builder()
			.user(user)
			.amount(request.getAmount())
			.paymentUuid(UUID.randomUUID().toString())
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.build();

		purchaseRepository.save(purchase);
		return new InitiatePaymentResponse(purchase.getId(), purchase.getPaymentStatus().name());
	}

	/**
	 * 결제 승인
	 * - 결제 승인 시 결제 정보 반환
	 *
	 * @param request 결제 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId) throws
		IOException,
		InterruptedException {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));

		if (!Objects.equals(purchase.getAmount(), request.getAmount())) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}

		Long eventId = redisLockService.extractEventIdByUserId(userId);
		List<Long> seatIds = redisLockService.getLockedSeatIdsByUserId(userId);

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("이벤트 정보를 찾을 수 없습니다."));

		List<Seat> seats = seatRepository.findAllById(seatIds);
		if (seats.size() != seatIds.size()) {
			throw new IllegalStateException("일부 좌석을 찾을 수 없습니다.");
		}
		seats.forEach(seat -> seat.setAvailable(false));

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			request.getPaymentKey(), request.getOrderId(), request.getAmount()
		);

		PaymentMethodEnum methodEnum = PaymentMethodEnum.from(info.getMethod());

		purchase.updatePaymentInfo(
			info.getPaymentKey(),
			info.getTotalAmount(),
			methodEnum,
			PaymentStatusEnum.DONE,
			event.getSeatSelectable() ? "지정석 %d매".formatted(seatIds.size()) : "미지정석 %d매".formatted(seatIds.size()),
			info.getApprovedAt().toLocalDateTime()
		);

		List<Ticket> tickets = seats.stream()
			.map(seat -> {
				seat.reserve();
				Ticket ticket = new Ticket(null, seat.getLocation(), LocalDateTime.now(), event, purchase);
				seat.setTicket(ticket);
				return ticket;
			})
			.toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(seats);
		purchaseRepository.save(purchase);

		redisLockService.releaseAllLocks(userId);

		return new ConfirmPaymentResponse(
			info.getPaymentKey(),
			info.getOrderId(),
			purchase.getOrderName(),
			info.getTotalAmount(),
			info.getStatus(),
			methodEnum,
			info.getApprovedAt().toLocalDateTime(),
			info.getReceipt()
		);
	}

	/**
	 * 사용자의 구매 이력을 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @return 구매 이력 응답 DTO
	 */
	public PurchaseHistoryResponse getPurchaseHistory(Long userId) {
		List<Purchase> purchases = purchaseRepository.findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(
			userId,
			List.of(PaymentStatusEnum.DONE, PaymentStatusEnum.EXPIRED)
		);

		// 구매 이력 목록의 각 구매 객체를 PurchaseDto로 변환
		List<PurchaseHistoryResponse.PurchaseDto> purchaseDtos = purchases.stream()
			.map(purchase -> {
				// 첫 번째 티켓에서 이벤트 ID를 가져옴 (모든 티켓은 같은 이벤트에 속함) -> 이게 최선일까?
				Long eventId = purchase.getTickets().isEmpty() ? null :
					purchase.getTickets().get(0).getEvent().getEventId();

				return PurchaseHistoryResponse.PurchaseDto.builder()
					.purchaseId(purchase.getId())
					.eventId(eventId)
					.itemName(purchase.getOrderName())
					.amount(purchase.getAmount())
					.purchaseDate(purchase.getPurchaseDate())
					.paymentMethod(purchase.getPaymentMethod().name())
					.paymentStatus(purchase.getPaymentStatus().name())
					.tickets(purchase.getTickets().stream()
						.map(ticket -> PurchaseHistoryResponse.TicketInfo.builder()
							.ticketId(ticket.getId())
							.seatLocation(ticket.getSeatInfo())
							.build())
						.toList())
					.build();
			})
			.toList();

		return PurchaseHistoryResponse.builder()
			.purchases(purchaseDtos)
			.build();
	}
}