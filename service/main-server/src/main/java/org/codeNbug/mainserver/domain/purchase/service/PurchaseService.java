package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryDetailResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryListResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.entity.PurchaseCancel;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseCancelRepository;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.seat.service.RedisLockService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.dto.CanceledPaymentInfo;
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
	private final PurchaseCancelRepository purchaseCancelRepository;
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
	 * - 결제 승인 시 구매 정보를 갱신하고 티켓을 생성한 후 결제 응답을 반환합니다.
	 *
	 * @param request    결제 승인 결과 정보
	 * @param userId     현재 로그인한 사용자 ID
	 * @return 결제 UUID, 금액, 결제 수단, 승인 시각 등을 포함한 응답 DTO
	 */
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId) throws
		IOException, InterruptedException {
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

		LocalDateTime localDateTime = OffsetDateTime.parse(info.getApprovedAt())
			.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
			.toLocalDateTime();

		purchase.updatePaymentInfo(
			info.getPaymentKey(),
			info.getOrderId(),
			info.getTotalAmount(),
			methodEnum,
			event.getSeatSelectable() ? "지정석 %d매".formatted(seatIds.size()) :
				"미지정석 %d매".formatted(seatIds.size()),
			localDateTime
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
		redisLockService.releaseAllEntryQueueLocks(userId);

		return new ConfirmPaymentResponse(
			info.getPaymentKey(),
			info.getOrderId(),
			purchase.getOrderName(),
			info.getTotalAmount(),
			info.getStatus(),
			methodEnum,
			localDateTime,
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
			purchase.getTickets().getFirst().getEvent().getEventId();

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

	/**
	 * 유저 측 티켓 결제 취소
	 * - 전액 또는 부분 취소 요청 시 Toss 결제 취소 API 호출
	 * - 결제 취소 결과 정보를 반환
	 *
	 * @param paymentKey 결제 uuid 키
	 * @param request 결제 취소 정보가 포함된 요청 DTO (사유, 부분 취소 금액 등)
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 취소 상태 정보를 포함한 응답 DTO
	 */
	public CancelPaymentResponse cancelPayment(CancelPaymentRequest request, String paymentKey, Long userId) {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("[confirm] 사용자가 존재하지 않습니다."));

		Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("해당 결제 정보를 찾을 수 없습니다."));

		CanceledPaymentInfo canceledPaymentInfo = request.getCancelAmount() == null
			? tossPaymentService.cancelPayment(paymentKey, request.getCancelReason())
			: tossPaymentService.cancelPartialPayment(paymentKey, request.getCancelReason(), request.getCancelAmount());

		List<Ticket> tickets = ticketRepository.findAllByPurchaseId(purchase.getId());
		for (Ticket ticket : tickets) {
			List<Seat> seats = seatRepository.findByTicketId(ticket.getId());
			for (Seat seat : seats) {
				seat.setTicket(null);
				seat.setAvailable(true);
				seatRepository.save(seat);
			}

			ticketRepository.delete(ticket);
		}

		for (CanceledPaymentInfo.CancelDetail cancelDetail : canceledPaymentInfo.getCancels()) {
			boolean isPartial = canceledPaymentInfo.getBalanceAmount() > 0;

			PurchaseCancel purchaseCancel = PurchaseCancel.builder()
				.purchase(purchase)
				.cancelAmount(cancelDetail.getCancelAmount())
				.cancelReason(cancelDetail.getCancelReason())
				.canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
				.receiptUrl(canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
				.isPartial(isPartial)
				.build();

			purchaseCancelRepository.save(purchaseCancel);
		}

		return CancelPaymentResponse.builder()
			.paymentKey(canceledPaymentInfo.getPaymentKey())
			.orderId(canceledPaymentInfo.getOrderId())
			.status(canceledPaymentInfo.getStatus())
			.method(canceledPaymentInfo.getMethod())
			.totalAmount(canceledPaymentInfo.getTotalAmount())
			.balanceAmount(canceledPaymentInfo.getBalanceAmount())
			.isPartialCancelable(canceledPaymentInfo.getIsPartialCancelable())
			.receiptUrl(canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
			.cancels(canceledPaymentInfo.getCancels().stream()
				.map(c -> CancelPaymentResponse.CancelDetail.builder()
					.cancelAmount(c.getCancelAmount())
					.canceledAt(OffsetDateTime.parse(c.getCanceledAt()).toLocalDateTime())
					.cancelReason(c.getCancelReason())
					.build())
				.toList())
			.build();
	}
}