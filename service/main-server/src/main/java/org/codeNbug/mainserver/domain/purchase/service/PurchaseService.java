package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.dto.ManagerRefundRequest;
import org.codeNbug.mainserver.domain.manager.dto.ManagerRefundResponse;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
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
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final ManagerEventRepository managerEventRepository;
	private final NotificationService notificationService;

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
		IOException,
		InterruptedException {
		try {
			Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
				.orElseThrow(() -> new IllegalArgumentException("[confirm] 구매 정보를 찾을 수 없습니다."));

			if (request.getAmount().equals(0)) {
				throw new BadRequestException("[confirm] 결제 금액이 0원입니다.");
			}

			if (!Objects.equals(purchase.getAmount(), request.getAmount())) {
				throw new BadRequestException("[confirm] 결제 금액이 일치하지 않습니다.");
			}

			Long eventId = redisLockService.extractEventIdByUserId(userId);
			List<Long> seatIds = redisLockService.getLockedSeatIdsByUserId(userId);

			Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("[confirm] 이벤트 정보를 찾을 수 없습니다."));

			List<Seat> seats = seatRepository.findAllById(seatIds);
			if (seats.size() != seatIds.size()) {
				throw new BadRequestException("[confirm] 일부 좌석을 찾을 수 없습니다.");
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

			// 결제 완료 알림 생성
			try {
				String notificationTitle = String.format("[%s] 결제 완료", purchase.getOrderName());
				String notificationContent = String.format("결제가 완료되었습니다.\n금액: %d원\n결제수단: %s",
					purchase.getAmount(),
					methodEnum.name()
				);
				String targetUrl = String.format("/my");

				notificationService.createNotification(userId, NotificationEnum.PAYMENT, notificationTitle,
					notificationContent, targetUrl);
			} catch (Exception e) {
				log.error("결제 완료 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
					userId, purchase.getId(), e.getMessage(), e);
				// 알림 발송 실패는 결제 성공에 영향을 주지 않도록 예외를 무시함
			}

			return new ConfirmPaymentResponse(
				info.getPaymentKey(),
				info.getOrderId(),
				purchase.getOrderName(),
				info.getTotalAmount(),
				info.getStatus(),
				methodEnum,
				localDateTime,
				new ConfirmPaymentResponse.Receipt(info.getReceipt().getUrl())
			);
		} catch (Exception e) {
			log.error("[confirmPayment] 결제 처리 중 예외 발생 - userId: {}, 오류: {}", userId, e.getMessage(), e);
			e.printStackTrace();
			throw e;
		} finally {
			redisLockService.releaseAllLocks(userId);
			redisLockService.releaseAllEntryQueueLocks(userId);
		}
	}

	/**
	 * 사용자의 구매 이력 목록을 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @param pageable 페이지네이션 정보
	 * @return 구매 이력 목록 응답 DTO
	 */
	@Transactional
	public PurchaseHistoryListResponse getPurchaseHistoryList(Long userId, Pageable pageable) {
		Page<Purchase> purchases = purchaseRepository.findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(
			userId,
			List.of(PaymentStatusEnum.DONE, PaymentStatusEnum.EXPIRED),
			pageable
		);

		Page<PurchaseHistoryListResponse.PurchaseSummaryDto> purchaseDtos = purchases.map(purchase ->
			PurchaseHistoryListResponse.PurchaseSummaryDto.builder()
				.purchaseId(purchase.getId())
				.itemName(purchase.getOrderName())
				.amount(purchase.getAmount())
				.purchaseDate(purchase.getPurchaseDate())
				.paymentMethod(purchase.getPaymentMethod().name())
				.paymentStatus(purchase.getPaymentStatus().name())
				.build()
		);

		return PurchaseHistoryListResponse.of(purchaseDtos);
	}

	/**
	 * 사용자의 특정 구매 이력 상세 정보를 조회합니다.
	 *
	 * @param userId 사용자 ID
	 * @param purchaseId 구매 ID
	 * @return 구매 이력 상세 응답 DTO
	 */
	@Transactional
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
			.paymentKey(purchase.getPaymentUuid())
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

		log.info("purchase paymentKey: {}", purchaseDto.getPaymentKey());

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
	 * @param request 결제 취소 사유가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 취소 상태 정보를 포함한 응답 DTO
	 */
	public CancelPaymentResponse cancelPayment(CancelPaymentRequest request, String paymentKey, Long userId) {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("[cancel] 사용자가 존재하지 않습니다."));

		Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("[cancel] 해당 결제 정보를 찾을 수 없습니다."));

		CanceledPaymentInfo canceledPaymentInfo = tossPaymentService.cancelPayment(paymentKey,
			request.getCancelReason());

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
			PurchaseCancel purchaseCancel = PurchaseCancel.builder()
				.purchase(purchase)
				.cancelAmount(cancelDetail.getCancelAmount())
				.cancelReason(cancelDetail.getCancelReason())
				.canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
				.receiptUrl(canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
				.build();

			purchaseCancelRepository.save(purchaseCancel);
		}

		// 환불 완료 알림 생성
		try {
			int refundAmount = canceledPaymentInfo.getCancels().stream()
				.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
				.sum();

			String notificationTitle = String.format("[%s] 환불 완료", purchase.getOrderName());
			String notificationContent = String.format(
				"환불 처리가 완료되었습니다.\n환불 금액: %d원",
				refundAmount
			);
			String targetUrl = String.format("/my");

			notificationService.createNotification(userId, NotificationEnum.PAYMENT, notificationTitle,
				notificationContent, targetUrl);
		} catch (Exception e) {
			log.error("환불 완료 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
				userId, purchase.getId(), e.getMessage(), e);
			// 알림 발송 실패는 환불 처리에 영향을 주지 않도록 예외를 무시함
		}

		return CancelPaymentResponse.builder()
			.paymentKey(canceledPaymentInfo.getPaymentKey())
			.orderId(canceledPaymentInfo.getOrderId())
			.status(canceledPaymentInfo.getStatus())
			.method(canceledPaymentInfo.getMethod())
			.totalAmount(canceledPaymentInfo.getTotalAmount())
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

	@Transactional
	public List<ManagerRefundResponse> managerCancelPayment(ManagerRefundRequest request, Long eventId, User manager) {
		List<Event> eventsByManager = managerEventRepository.findEventsByManager(manager);

		boolean hasPermission = eventsByManager.stream()
			.anyMatch(event -> event.getEventId().equals(eventId));

		if (!hasPermission) {
			throw new IllegalArgumentException("요청 매니저는 해당 이벤트에 대한 권한이 없습니다.");
		}

		// 환불할 purchaseId 목록 결정
		List<Purchase> purchasesToRefund;
		if (request.isTotalRefund()) {
			purchasesToRefund = purchaseRepository.findAllByEventId(eventId);
		} else {
			purchasesToRefund = request.getPurchasesIds().stream()
				.map(id -> purchaseRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("해당 구매 이력이 존재하지 않습니다. ID: " + id)))
				.filter(p -> {
					Long ticketEventId = p.getTickets().getFirst().getEvent().getEventId();
					if (!ticketEventId.equals(eventId)) {
						throw new IllegalArgumentException("요청한 매니저의 이벤트와 결제 티켓의 이벤트가 일치하지 않습니다.");
					}
					return true;
				})
				.toList();
		}

		List<ManagerRefundResponse> responseList = new ArrayList<>();

		for (Purchase purchase : purchasesToRefund) {
			// Toss 결제 취소
			CanceledPaymentInfo canceledPaymentInfo = tossPaymentService.cancelPayment(
				purchase.getPaymentUuid(),
				request.getReason()
			);

			// 좌석 초기화 및 티켓 삭제
			List<Ticket> tickets = ticketRepository.findAllByPurchaseId(purchase.getId());
			List<Long> ticketIds = new ArrayList<>();

			for (Ticket ticket : tickets) {
				ticketIds.add(ticket.getId());

				List<Seat> seats = seatRepository.findByTicketId(ticket.getId());
				for (Seat seat : seats) {
					seat.setTicket(null);
					seat.setAvailable(true);
					seatRepository.save(seat);
				}
				ticketRepository.delete(ticket);
			}

			// PurchaseCancel 저장
			for (CanceledPaymentInfo.CancelDetail cancelDetail : canceledPaymentInfo.getCancels()) {
				PurchaseCancel purchaseCancel = PurchaseCancel.builder()
					.purchase(purchase)
					.cancelAmount(cancelDetail.getCancelAmount())
					.cancelReason(cancelDetail.getCancelReason())
					.canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
					.receiptUrl(
						canceledPaymentInfo.getReceipt() != null ? canceledPaymentInfo.getReceipt().getUrl() : null)
					.build();

				purchaseCancelRepository.save(purchaseCancel);
			}

			// 각 사용자에게 환불 알림 전송
			try {
				int refundAmount = canceledPaymentInfo.getCancels().stream()
					.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
					.sum();

				String notificationTitle = String.format("[%s] 매니저 환불 처리", purchase.getOrderName());
				String notificationContent = String.format(
					"매니저에 의해 환불이 처리되었습니다.\n사유: %s\n환불 금액: %d원",
					request.getReason(),
					refundAmount
				);

				// 각 구매자에게 개별 알림 전송
				notificationService.createNotification(
					purchase.getUser().getUserId(),
					NotificationEnum.PAYMENT,
					notificationTitle,
					notificationContent
				);
			} catch (Exception e) {
				log.error("매니저 환불 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
					purchase.getUser().getUserId(), purchase.getId(), e.getMessage(), e);
				// 알림 발송 실패는 환불 처리에 영향을 주지 않도록 예외를 무시함
			}

			// 응답 DTO 생성
			ManagerRefundResponse response = ManagerRefundResponse.builder()
				.purchaseId(purchase.getId())
				.userId(purchase.getUser().getUserId())
				.paymentStatus(purchase.getPaymentStatus())
				.ticketId(ticketIds)
				.refundAmount(canceledPaymentInfo.getCancels().stream()
					.mapToInt(CanceledPaymentInfo.CancelDetail::getCancelAmount)
					.sum())
				.refundDate(OffsetDateTime.parse(
					canceledPaymentInfo.getCancels().getLast().getCanceledAt()
				).toLocalDateTime())
				.build();

			responseList.add(response);
		}

		return responseList;
	}

}