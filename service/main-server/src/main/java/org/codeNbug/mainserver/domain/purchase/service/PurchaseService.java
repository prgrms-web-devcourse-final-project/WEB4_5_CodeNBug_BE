package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.util.UUID;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.TicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {
	private final PurchaseRepository purchaseRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;
	private final TossPaymentService tossPaymentService;

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
			.orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));
		eventRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("행사가 존재하지 않습니다."));

		String uuid = UUID.randomUUID().toString();

		String orderName = (request.getSeatIds() != null && !request.getSeatIds().isEmpty())
			? "지정석 " + request.getSeatIds().size() + "매"
			: "미지정석 " + request.getTicketCount() + "매";

		Purchase purchase = Purchase.builder()
			.paymentUuid(uuid)
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.amount(request.getAmount())
			.user(user)
			.eventId(request.getEventId())
			.ticketCount(request.getTicketCount())
			.selectedSeatIds(
				request.getSeatIds() != null && !request.getSeatIds().isEmpty()
					? request.getSeatIds()
					: null
			)
			.orderName(orderName)
			.build();

		purchaseRepository.save(purchase);

		return new InitiatePaymentResponse(purchase.getId(), purchase.getPaymentStatus().name());
	}

	public ConfirmPaymentResponse confirmPayment(String paymentKey, String orderId, int amount)
		throws IOException, InterruptedException {

		Purchase purchase = purchaseRepository.findByOrderId(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

		if (purchase.getAmount() != amount) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			paymentKey, orderId, purchase.getOrderName(), amount
		);

		PaymentMethodEnum methodEnum = PaymentMethodEnum.valueOf(info.getMethod().toUpperCase());

		purchase.updatePaymentInfo(
			info.getPaymentKey(),
			info.getTotalAmount(),
			methodEnum,
			PaymentStatusEnum.DONE,
			info.getOrderName(),
			info.getApprovedAt()
		);

		// 티켓 발급
		Event event = eventRepository.findById(purchase.getEventId())
			.orElseThrow(() -> new IllegalStateException("이벤트 정보를 찾을 수 없습니다."));
		User user = purchase.getUser();

		TicketPurchaseRequest request = event.getSeatSelectable()
			? new SelectTicketPurchaseRequest(purchase.getId(), event.getEventId(), purchase.getSelectedSeatIds())
			: new NonSelectTicketPurchaseRequest(purchase.getId(), event.getEventId(), purchase.getTicketCount());

		// 응답 DTO 구성
		ConfirmPaymentResponse.Receipt receipt = new ConfirmPaymentResponse.Receipt(info.getReceipt().getUrl());

		return new ConfirmPaymentResponse(
			info.getPaymentKey(),
			info.getOrderId(),
			info.getOrderName(),
			info.getTotalAmount(),
			info.getStatus(),
			methodEnum,
			info.getApprovedAt(),
			receipt
		);
	}

	/**
	 * 결제 정보 반환
	 * - 결제 승인 시 결제 정보 반환
	 *
	 * @param paymentKey 결제 paymentUuid
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	// public TicketPurchaseResponse getPaymentInfo(String paymentKey) {
	// 	Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
	// 		.orElseThrow(() -> new IllegalStateException("해당 결제를 찾을 수 없습니다."));
	//
	// 	if (purchase.getPaymentStatus() != PaymentStatusEnum.DONE) {
	// 		throw new IllegalStateException("결제가 아직 완료되지 않았습니다.");
	// 	}
	//
	// 	List<Ticket> tickets = purchase.getTickets();
	// 	if (tickets.isEmpty()) {
	// 		throw new IllegalStateException("티켓 정보가 존재하지 않습니다.");
	// 	}
	//
	// 	Event event = tickets.get(0).getEvent();
	// 	User user = purchase.getUser();
	//
	// 	List<TicketPurchaseResponse.TicketInfo> ticketInfos = tickets.stream()
	// 		.map(ticket -> {
	// 			Seat seat = seatRepository.findByLocation(ticket.getSeatInfo())
	// 				.orElseThrow(() -> new IllegalStateException("해당 좌석을 찾을 수 없습니다."));
	// 			return new TicketPurchaseResponse.TicketInfo(ticket.getId(), seat.getId());
	// 		})
	// 		.toList();
	//
	// 	return TicketPurchaseResponse.builder()
	// 		.purchaseId(purchase.getId())
	// 		.eventId(event.getEventId())
	// 		.userId(user.getUserId())
	// 		.ticketCount(tickets.size())
	// 		.amount(purchase.getAmount())
	// 		.paymentStatus(purchase.getPaymentStatus().name())
	// 		.purchaseDate(purchase.getPurchaseDate())
	// 		.tickets(ticketInfos)
	// 		.build();
	// }
}