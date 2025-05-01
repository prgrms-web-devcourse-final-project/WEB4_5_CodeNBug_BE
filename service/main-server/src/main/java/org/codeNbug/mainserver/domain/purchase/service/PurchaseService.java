package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryResponse;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentServiceImpl;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {

	private final TossPaymentServiceImpl tossPaymentService;
	private final PurchaseRepository purchaseRepository;
	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;

	/**
	 * 결제 사전 등록 처리
	 * - 결제 UUID를 생성하고 결제 상태를 '진행 중'으로 설정하여 저장
	 *
	 * @param request 이벤트 ID 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Long userId) {
		User user = tossPaymentService.getUser(userId);
		tossPaymentService.getEvent(request.getEventId());

		String uuid = UUID.randomUUID().toString();

		Purchase purchase = Purchase.builder()
			.paymentUuid(uuid)
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.amount(request.getAmount())
			.user(user)
			.build();

		purchaseRepository.save(purchase);

		return new InitiatePaymentResponse(purchase.getId(), purchase.getPaymentStatus().name());
	}

	/**
	 * 미지정석 티켓 결제 처리
	 * - Toss 결제 승인 → 결제 정보 업데이트 → 사용 가능한 좌석 자동 배정 → 티켓 생성
	 *
	 * @param request 미지정석 티켓 결제 요청 정보
	 * @param userId 로그인한 사용자 ID
	 * @return 구매 완료 응답 DTO
	 * @throws IOException Toss API 호출 실패 시
	 * @throws InterruptedException Toss API 호출 실패 시
	 */
	@Transactional
	public NonSelectTicketPurchaseResponse purchaseNonSelectTicket(NonSelectTicketPurchaseRequest request, Long userId)
		throws IOException, InterruptedException {

		confirmAndUpdatePurchase(request);

		Event event = tossPaymentService.getEvent(request.getEventId());
		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalStateException("등록된 사전 결제가 없습니다."));

		List<Seat> availableSeats = seatRepository.findAvailableSeatsByEventId(event.getEventId());
		if (availableSeats.size() < request.getTicketCount()) {
			throw new IllegalStateException("선택 가능한 좌석이 부족합니다.");
		}

		int approvedAmount = purchaseRepository.findAmountById(purchase.getId());
		int expectedAmount = purchase.getAmount();

		if (expectedAmount != approvedAmount) {
			throw new IllegalStateException("결제 금액이 일치하지 않습니다. 예상: " + expectedAmount + ", 승인됨: " + approvedAmount);
		}

		List<Ticket> tickets = IntStream.range(0, request.getTicketCount())
			.mapToObj(i -> {
				Seat seat = availableSeats.get(i);
				seat.reserve();
				Ticket ticket = new Ticket(
					null,
					seat.getLocation(),
					LocalDateTime.now(),
					event,
					purchase
				);
				seat.setTicket(ticket);
				purchase.addTicket(ticket);
				return ticket;
			})
			.toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(availableSeats.subList(0, request.getTicketCount()));

		User user = tossPaymentService.getUser(userId);

		return NonSelectTicketPurchaseResponse.builder()
			.purchaseId(purchase.getId())
			.eventId(event.getEventId())
			.userId(user.getUserId())
			.tickets(tickets.stream()
				.map(t -> new NonSelectTicketPurchaseResponse.TicketInfo(t.getId()))
				.toList())
			.ticketCount(tickets.size())
			.amount(purchase.getAmount())
			.paymentStatus(purchase.getPaymentStatus().name())
			.purchaseDate(purchase.getPurchaseDate())
			.build();
	}

	/**
	 * 지정석 티켓 결제 처리
	 * - Toss 결제 승인 → 좌석 유효성 및 예약 처리 → 결제 정보 업데이트 → 티켓 생성
	 *
	 * @param request 지정석 결제 요청 정보 (좌석 ID 포함)
	 * @param userId 로그인한 사용자 ID
	 * @return 구매 완료 응답 DTO
	 * @throws IOException Toss API 호출 실패 시
	 * @throws InterruptedException Toss API 호출 실패 시
	 */
	@Transactional
	public SelectTicketPurchaseResponse purchaseSelectedSeats(SelectTicketPurchaseRequest request, Long userId)
		throws IOException, InterruptedException {

		confirmAndUpdatePurchase(request);

		Event event = tossPaymentService.getEvent(request.getEventId());
		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalStateException("등록된 사전 결제가 없습니다."));

		List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
		if (seats.size() != request.getSeatIds().size()) {
			throw new IllegalStateException("존재하지 않는 좌석이 포함되어 있습니다.");
		}
		for (Seat seat : seats) {
			if (!seat.isAvailable()) {
				throw new IllegalStateException("선택한 좌석 중 매진된 좌석이 있습니다: " + seat.getId());
			}
			seat.reserve();
		}

		List<Ticket> tickets = seats.stream()
			.map(seat -> {
				Ticket ticket = new Ticket(null, seat.getLocation(), LocalDateTime.now(), event, purchase);
				seat.setTicket(ticket);
				purchase.addTicket(ticket);
				return ticket;
			})
			.toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(seats);

		User user = tossPaymentService.getUser(userId);

		return SelectTicketPurchaseResponse.builder()
			.purchaseId(purchase.getId())
			.eventId(event.getEventId())
			.userId(user.getUserId())
			.tickets(IntStream.range(0, tickets.size())
				.mapToObj(i -> new SelectTicketPurchaseResponse.TicketInfo(
					tickets.get(i).getId(),
					seats.get(i).getId()
				))
				.toList())
			.ticketCount(tickets.size())
			.amount(purchase.getAmount())
			.paymentStatus(purchase.getPaymentStatus().name())
			.purchaseDate(purchase.getPurchaseDate())
			.build();
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

  /**
	 * Toss 결제 승인 요청 및 구매 정보 업데이트 공통 처리 메서드
	 *
	 * @param request 결제 요청 정보 (지정석/미지정석 공통)
	 * @throws IOException          Toss API 호출 실패 시
	 * @throws InterruptedException Toss API 호출 실패 시
	 */
	private void confirmAndUpdatePurchase(Object request)
		throws IOException, InterruptedException {

		String paymentUuid;
		String orderId;
		String orderName;
		Integer amount;
		String paymentMethod;
		Long purchaseId;
		String status = "IN_PROGRESS";

		if (request instanceof NonSelectTicketPurchaseRequest nonSelect) {
			paymentUuid = nonSelect.getPaymentUuid();
			orderId = nonSelect.getOrderId();
			orderName = nonSelect.getOrderName();
			amount = nonSelect.getAmount();
			paymentMethod = nonSelect.getPaymentMethod();
			purchaseId = nonSelect.getPurchaseId();
		} else if (request instanceof SelectTicketPurchaseRequest select) {
			paymentUuid = select.getPaymentUuid();
			orderId = select.getOrderId();
			orderName = select.getOrderName();
			amount = select.getAmount();
			paymentMethod = select.getPaymentMethod();
			purchaseId = select.getPurchaseId();
		} else {
			throw new IllegalArgumentException("지원하지 않는 결제 요청 타입입니다.");
		}

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			paymentUuid, orderId, orderName, amount, status
		);

		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalStateException("등록된 사전 결제가 없습니다."));

		PaymentStatusEnum statusEnum = PaymentStatusEnum.valueOf(info.getStatus());
		if (statusEnum == PaymentStatusEnum.EXPIRED) {
			throw new IllegalStateException("결제 유효 시간이 초과되었습니다.");
		}

		purchase.updatePaymentInfo(
			paymentUuid,
			Integer.parseInt(info.getTotalAmount()),
			PaymentMethodEnum.valueOf(paymentMethod),
			PaymentStatusEnum.valueOf(info.getStatus()),
			orderName,
			OffsetDateTime.parse(info.getApprovedAt()).toLocalDateTime()
		);
	}
}