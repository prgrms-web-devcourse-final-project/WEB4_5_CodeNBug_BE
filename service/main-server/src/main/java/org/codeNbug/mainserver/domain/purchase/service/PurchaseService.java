package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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

		String redisPrefix = "seat:lock:" + userId + ":";
		Set<String> seatKeys = redisTemplate.keys(redisPrefix + "*");

		if (seatKeys.isEmpty()) {
			throw new IllegalStateException("선택된 좌석 정보가 존재하지 않습니다.");
		}

		String firstKey = seatKeys.iterator().next();
		String[] parts = firstKey.split(":");
		if (parts.length < 4) {
			throw new IllegalStateException("좌석 키 형식 오류: " + firstKey);
		}
		Long eventId = Long.parseLong(parts[3]);

		eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalStateException("행사가 존재하지 않습니다."));

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
	 * 결제 승인
	 * - 결제 승인 시 결제 정보 반환
	 *
	 * @param request 결제 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	@Transactional
	public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId)
		throws IOException, InterruptedException {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));
		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
		if (!Objects.equals(purchase.getAmount(), request.getAmount())) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}

		String redisPrefix = "seat:lock:" + userId + ":";
		Set<String> seatKeys = redisKeyScanner.scanKeys(redisPrefix + "*");
		String firstKey = seatKeys.iterator().next();
		String[] firstParts = firstKey.split(":");

		if (seatKeys.isEmpty()) {
			throw new IllegalStateException("선택된 좌석 정보가 존재하지 않습니다.");
		}

		List<Long> seatIds = seatKeys.stream()
			.map(key -> {
				String[] parts = key.split(":");
				if (parts.length != 5)
					throw new IllegalStateException("좌석 키 형식 오류: " + key);
				return Long.parseLong(parts[4]); // seatId
			})
			.toList();

		Long eventId = Long.parseLong(firstParts[3]);

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalStateException("이벤트 정보를 찾을 수 없습니다."));

		List<Seat> selectedSeats = seatRepository.findAllById(seatIds);
		if (selectedSeats.size() != seatIds.size()) {
			throw new IllegalStateException("일부 좌석을 찾을 수 없습니다.");
		}

		for (Seat seat : selectedSeats) {
			seat.setAvailable(false);
		}

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			request.getPaymentKey(), request.getOrderId(), request.getAmount()
		);

		PaymentMethodEnum methodEnum = PaymentMethodEnum.from(info.getMethod());

		String orderName = event.getSeatSelectable()
			? "지정석 " + selectedSeats.size() + "매"
			: "미지정석 " + selectedSeats.size() + "매";

		purchase.updatePaymentInfo(
			info.getPaymentKey(),
			info.getTotalAmount(),
			methodEnum,
			PaymentStatusEnum.DONE,
			orderName,
			info.getApprovedAt().toLocalDateTime()
		);

		List<Ticket> tickets = selectedSeats.stream()
			.map(seat -> {
				seat.reserve();
				Ticket ticket = new Ticket(null, seat.getLocation(), LocalDateTime.now(), event, purchase);
				seat.setTicket(ticket);
				return ticket;
			}).toList();

		purchaseRepository.save(purchase);
		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(selectedSeats);

		return new ConfirmPaymentResponse(
			info.getPaymentKey(),
			info.getOrderId(),
			orderName,
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