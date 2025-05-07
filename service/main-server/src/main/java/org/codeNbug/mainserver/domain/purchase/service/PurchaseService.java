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
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryDetailResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryListResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
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
			.orElseThrow(() -> new IllegalArgumentException("[init] 사용자가 존재하지 않습니다."));

		Long eventId = redisLockService.extractEventIdByUserId(userId);

		eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("[init] 행사가 존재하지 않습니다."));

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
			.orElseThrow(() -> new IllegalArgumentException("[confirm] 사용자가 존재하지 않습니다."));

		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalArgumentException("[confirm] 구매 정보를 찾을 수 없습니다."));

		if (!Objects.equals(purchase.getAmount(), request.getAmount())) {
			throw new IllegalArgumentException("[confirm] 결제 금액이 일치하지 않습니다.");
		}

		Long eventId = redisLockService.extractEventIdByUserId(userId);
		List<Long> seatIds = redisLockService.getLockedSeatIdsByUserId(userId);

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("[confirm] 이벤트 정보를 찾을 수 없습니다."));

		List<Seat> seats = seatRepository.findAllById(seatIds);
		if (seats.size() != seatIds.size()) {
			throw new IllegalStateException("[confirm] 일부 좌석을 찾을 수 없습니다.");
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
			event.getSeatSelectable() ? "지정석 %d매".formatted(seatIds.size()) :
				"미지정석 %d매".formatted(seatIds.size()),
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
	 * 사용자의 구매 이력 목록을 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @return 구매 이력 목록 응답 DTO
	 */
	public PurchaseHistoryListResponse getPurchaseHistoryList(Long userId) {
		List<Purchase> purchases = purchaseRepository.findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(
			userId,
			List.of(PaymentStatusEnum.DONE, PaymentStatusEnum.EXPIRED)
		);

		List<PurchaseHistoryListResponse.PurchaseSummaryDto> purchaseDtos = purchases.stream()
			.map(purchase -> PurchaseHistoryListResponse.PurchaseSummaryDto.builder()
				.purchaseId(purchase.getId())
				.itemName(purchase.getOrderName())
				.amount(purchase.getAmount())
				.purchaseDate(purchase.getPurchaseDate())
				.paymentMethod(purchase.getPaymentMethod().name())
				.paymentStatus(purchase.getPaymentStatus().name())
				.build())
			.toList();

		return PurchaseHistoryListResponse.builder()
			.purchases(purchaseDtos)
			.build();
	}

	/**
	 * 사용자의 특정 구매 이력 상세 정보를 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @param purchaseId 구매 ID
	 * @return 구매 이력 상세 응답 DTO
	 */
	public PurchaseHistoryDetailResponse getPurchaseHistoryDetail(Long userId, Long purchaseId) {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));

		if (!purchase.getUser().getUserId().equals(userId)) {
			throw new IllegalArgumentException("해당 구매 정보에 대한 접근 권한이 없습니다.");
		}

		Long eventId = purchase.getTickets().isEmpty() ? null :
			purchase.getTickets().get(0).getEvent().getEventId();

		PurchaseHistoryDetailResponse.PurchaseDto purchaseDto = PurchaseHistoryDetailResponse.PurchaseDto.builder()
			.purchaseId(purchase.getId())
			.eventId(eventId)
			.itemName(purchase.getOrderName())
			.amount(purchase.getAmount())
			.purchaseDate(purchase.getPurchaseDate())
			.paymentMethod(purchase.getPaymentMethod().name())
			.paymentStatus(purchase.getPaymentStatus().name())
			.tickets(purchase.getTickets().stream()
				.map(ticket -> PurchaseHistoryDetailResponse.TicketInfo.builder()
					.ticketId(ticket.getId())
					.seatLocation(ticket.getSeatInfo())
					.build())
				.toList())
			.build();

		return PurchaseHistoryDetailResponse.builder()
			.purchases(List.of(purchaseDto))
			.build();
	}
}