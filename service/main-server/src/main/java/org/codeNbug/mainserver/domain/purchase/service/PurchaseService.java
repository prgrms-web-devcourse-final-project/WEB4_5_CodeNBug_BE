package org.codeNbug.mainserver.domain.purchase.service;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseResponse;
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
import org.codeNbug.mainserver.external.toss.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.TossPaymentClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {

	private final TossPaymentClient tossPaymentClient;
	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
		Event event = eventRepository.findById(request.getEventId())
			.orElseThrow(() -> new IllegalArgumentException("이벤트가 존재하지 않습니다."));

		String uuid = UUID.randomUUID().toString();

		Purchase purchase = Purchase.builder()
			.paymentUuid(uuid)
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.user(user)
			.build();

		purchaseRepository.save(purchase);

		return new InitiatePaymentResponse(uuid, purchase.getPaymentStatus().name());
	}

	@Transactional
	public NonSelectTicketPurchaseResponse purchaseNonSelectTicket(NonSelectTicketPurchaseRequest request, Long userId)
		throws IOException, InterruptedException {

		// 1. Toss 결제 승인 요청
		HttpResponse<String> tossResponse = tossPaymentClient.requestConfirm(
			request.getPaymentUuid(),
			request.getOrderId(),
			request.getOrderName(),
			request.getAmount()
		);
		if (tossResponse.statusCode() != 200) {
			throw new IllegalStateException("Toss 결제 승인 실패: " + tossResponse.body());
		}

		// 2. Toss 응답 JSON 파싱
		ConfirmedPaymentInfo info = objectMapper.readValue(tossResponse.body(), ConfirmedPaymentInfo.class);

		// 3. 유저 및 이벤트 조회
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
		Event event = eventRepository.findById(request.getEventId())
			.orElseThrow(() -> new IllegalArgumentException("이벤트가 존재하지 않습니다."));

		// 4. Purchase 업데이트
		Purchase purchase = purchaseRepository.findByPaymentUuid(info.getPaymentUuid())
			.orElseThrow(() -> new IllegalStateException("사전 등록된 결제가 없습니다."));

		purchase.updatePaymentInfo(
			Integer.parseInt(info.getTotalAmount()),
			PaymentMethodEnum.valueOf(request.getPaymentMethod()),
			PaymentStatusEnum.valueOf(info.getStatus()),
			request.getOrderName(),
			LocalDateTime.parse(info.getApprovedAt())
		);

		// 5. 사용 가능한 좌석 조회 및 검증
		List<Seat> availableSeats = seatRepository.findAvailableSeatsByEventId(event.getEventId());
		if (availableSeats.size() < request.getTicketCount()) {
			throw new IllegalStateException("선택 가능한 좌석이 부족합니다.");
		}

		// 6. Ticket 생성 + Seat 할당 및 상태 변경
		List<Ticket> tickets = IntStream.range(0, request.getTicketCount())
			.mapToObj(i -> {
				Seat seat = availableSeats.get(i);
				seat.reserve();
				return new Ticket(
					null,
					seat.getLocation(),
					LocalDateTime.now(),
					event,
					purchase
				);
			})
			.toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(availableSeats.subList(0, request.getTicketCount()));

		// 6. 응답 반환
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
}
