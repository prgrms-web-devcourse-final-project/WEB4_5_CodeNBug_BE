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
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {
	private final TossPaymentService tossPaymentService;
	private final PurchaseRepository purchaseRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;
	private final TicketRepository ticketRepository;

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
			.orderName(orderName)
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
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId)
		throws IOException, InterruptedException {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));
		Purchase purchase = purchaseRepository.findByOrderId(request.getOrderId())
			.orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

		if (!Objects.equals(purchase.getAmount(), request.getAmount())) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			request.getPaymentKey(), request.getOrderId(), purchase.getOrderName(), request.getAmount()
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

		Event event = eventRepository.findById(request.getEventId())
			.orElseThrow(() -> new IllegalStateException("이벤트 정보를 찾을 수 없습니다."));

		List<Seat> selectedSeats;

		if (event.getSeatSelectable()) {
			// 지정석일 경우
			selectedSeats = seatRepository.findAllById(request.getSeatList());
			if (selectedSeats.size() != request.getSeatList().size()) {
				throw new IllegalStateException("좌석 일부를 찾을 수 없습니다.");
			}
		} else {
			// 미지정석일 경우
			selectedSeats = seatRepository.findAvailableSeatsByEventId(event.getEventId())
				.stream().limit(request.getTicketCount()).toList();
			if (selectedSeats.size() < request.getTicketCount()) {
				throw new IllegalStateException("예매 가능한 좌석 수가 부족합니다.");
			}
		}

		List<Ticket> tickets = selectedSeats.stream()
			.map(seat -> {
				seat.reserve();
				Ticket ticket = new Ticket(null, seat.getLocation(), LocalDateTime.now(), event, purchase);
				seat.setTicket(ticket);
				purchase.addTicket(ticket);
				return ticket;
			}).toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(selectedSeats);

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
}