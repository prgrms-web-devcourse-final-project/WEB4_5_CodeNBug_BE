package org.codeNbug.mainserver.external.toss.webhook.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.TicketPurchaseRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final TossPaymentService tossPaymentService;
	private final SeatRepository seatRepository;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;

	@Value("${payment.toss.secret-key}")
	private String TOSS_SECRET_KEY;

	@Transactional
	public void processWebhook(String payload, String signature) {
		try {
			if (!isValidSignature(payload, signature)) {
				throw new SecurityException("잘못된 Toss 서명입니다.");
			}

			JsonNode root = objectMapper.readTree(payload);
			String eventType = root.get("event").asText();
			String paymentKey = root.get("data").get("paymentKey").asText();

			log.info("Toss 웹훅 수신: event = {}, paymentKey = {}", eventType, paymentKey);

			Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
				.orElseThrow(() -> new IllegalStateException("해당 결제를 찾을 수 없습니다."));

			switch (eventType) {
				case "PAYMENT_APPROVED" -> handlePaymentApproved(purchase);
				case "PAYMENT_EXPIRED" -> handlePaymentExpired(purchase);
				case "PAYMENT_CANCELED" -> handlePaymentCanceled(purchase);
				default -> throw new IllegalArgumentException("지원하지 않는 웹훅 이벤트: " + eventType);
			}

			purchaseRepository.save(purchase);
		} catch (Exception e) {
			throw new RuntimeException("웹훅 처리 중 오류 발생: " + e.getMessage(), e);
		}
	}

	public void handlePaymentApproved(Purchase purchase) {
		if (purchase.getPaymentStatus() != PaymentStatusEnum.IN_PROGRESS) {
			log.info("이미 처리된 결제입니다.");
			return;
		}

		try {
			ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
				purchase.getPaymentUuid(),
				purchase.getOrderId(),
				purchase.getOrderName(),
				purchase.getAmount()
			);

			purchase.updatePaymentInfo(
				info.getPaymentUuid(),
				info.getTotalAmount(),
				info.getMethod(),
				PaymentStatusEnum.DONE,
				info.getApprovedAt()
			);

			Long eventId = purchase.getEventId();
			Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new IllegalStateException("해당 이벤트를 찾을 수 없습니다."));
			User user = purchase.getUser();

			if (event.getSeatSelectable()) {
				SelectTicketPurchaseRequest request = new SelectTicketPurchaseRequest(
					purchase.getId(),
					event.getEventId(),
					purchase.getSelectedSeatIds()
				);
				processTicketPurchase(request, user.getUserId());
			} else {
				NonSelectTicketPurchaseRequest request = new NonSelectTicketPurchaseRequest(
					purchase.getId(),
					event.getEventId(),
					purchase.getTicketCount()
				);
				processTicketPurchase(request, user.getUserId());
			}

		} catch (Exception e) {
			log.error("결제 승인 처리 실패", e);
			throw new RuntimeException("결제 승인 처리 실패", e);
		}
	}

	public void processTicketPurchase(TicketPurchaseRequest request, Long userId) {
		userRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));
		Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
			.orElseThrow(() -> new IllegalStateException("등록된 사전 결제가 없습니다."));
		Event event = eventRepository.findById(request.getEventId())
			.orElseThrow(() -> new IllegalStateException("해당 이벤트를 찾을 수 없습니다."));

		int approvedAmount = purchaseRepository.findAmountById(purchase.getId());
		if (approvedAmount != purchase.getAmount()) {
			throw new IllegalStateException("결제 금액 불일치");
		}

		List<Seat> availableSeats = seatRepository.findAvailableSeatsByEventId(event.getEventId());
		if (availableSeats.size() < request.getTicketCount()) {
			throw new IllegalStateException("예매 가능한 좌석 수가 부족합니다.");
		}

		List<Ticket> tickets = IntStream.range(0, request.getTicketCount())
			.mapToObj(i -> {
				Seat seat = availableSeats.get(i);
				seat.reserve();
				Ticket ticket = new Ticket(null, seat.getLocation(), LocalDateTime.now(), event, purchase);
				seat.setTicket(ticket);
				purchase.addTicket(ticket);
				return ticket;
			})
			.toList();

		ticketRepository.saveAll(tickets);
		seatRepository.saveAll(availableSeats.subList(0, request.getTicketCount()));
	}

	private void handlePaymentExpired(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.IN_PROGRESS) {
			log.warn("결제 만료 처리: IN_PROGRESS -> EXPIRED");

			resetSeatsToAvailable(purchase);

			purchase.setPaymentStatus(PaymentStatusEnum.EXPIRED);
		}
	}

	private void handlePaymentCanceled(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.DONE) {
			log.info("결제 취소 처리: DONE -> CANCELED");

			resetSeatsToAvailable(purchase);

			purchase.setPaymentStatus(PaymentStatusEnum.CANCELLED);
		}
	}

	private void resetSeatsToAvailable(Purchase purchase) {
		if (purchase.getSelectedSeatIds() != null) {
			for (Long seatId : purchase.getSelectedSeatIds()) {
				Seat seat = seatRepository.findById(seatId)
					.orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다."));
				seat.setAvailable(true);
				seat.setTicket(null);
				seatRepository.save(seat);
			}
		}
	}

	private boolean isValidSignature(String payload, String receivedSignature) {
		try {
			String algorithm = "HmacSHA256";
			SecretKeySpec keySpec = new SecretKeySpec(TOSS_SECRET_KEY.getBytes(StandardCharsets.UTF_8), algorithm);
			Mac mac = Mac.getInstance(algorithm);
			mac.init(keySpec);
			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			String expectedSignature = bytesToHex(hash);
			return expectedSignature.equals(receivedSignature);
		} catch (Exception e) {
			log.error("시그니처 검증 중 오류 발생", e);
			return false;
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
