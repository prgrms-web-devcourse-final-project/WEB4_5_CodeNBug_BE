package org.codeNbug.mainserver.external.toss.webhook.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.codeNbug.mainserver.external.toss.service.TossPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;
	private final SeatRepository seatRepository;
	private final TicketRepository ticketRepository;
	private final TossPaymentService tossPaymentService;

	@Value("${payment.toss.secret-key}")
	private String tossSecretKey;

	public void handleWebhook(String payload, String signature) throws Exception {
		try {
			if (!isValidSignature(payload, signature)) {
				throw new SecurityException("Toss 시그니처 검증 실패");
			}

			JsonNode root = objectMapper.readTree(payload);
			String eventType = root.path("event").asText();
			String paymentKey = root.path("data").path("paymentKey").asText();

			log.info("[Webhook] event: {}, paymentKey: {}", eventType, paymentKey);

			Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
				.orElseThrow(() -> new IllegalStateException("해당 결제를 찾을 수 없습니다."));

			switch (eventType) {
				case "PAYMENT_APPROVED" -> handleApproved(purchase);
				case "PAYMENT_CANCELED" -> handleCanceled(purchase);
				case "PAYMENT_EXPIRED" -> handleExpired(purchase);
				default -> log.warn("처리되지 않은 웹훅 이벤트: {}", eventType);
			}
		} catch (Exception e) {
			log.error("웹훅 처리 실패: ", e);
			throw new RuntimeException("웹훅 처리 실패: " + e.getMessage(), e);
		}
	}

	private boolean isValidSignature(String payload, String receivedSignature) {
		if (receivedSignature == null) {
			log.warn("테스트 환경으로 간주하고 시그니처 검증을 건너뜁니다.");
			return true;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(tossSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			String expectedSignature = bytesToHex(hash);
			return expectedSignature.equals(receivedSignature);
		} catch (Exception e) {
			log.error("시그니처 검증 오류", e);
			return false;
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1)
				builder.append("0");
			builder.append(hex);
		}
		return builder.toString();
	}

	private void handleApproved(Purchase purchase) throws IOException, InterruptedException {
		if (purchase.getPaymentStatus() != PaymentStatusEnum.IN_PROGRESS)
			return;

		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			purchase.getPaymentUuid(),
			purchase.getOrderId(),
			purchase.getAmount()
		);

		purchase.updatePaymentInfo(
			info.getPaymentKey(),
			info.getTotalAmount(),
			PaymentMethodEnum.valueOf(info.getMethod()),
			PaymentStatusEnum.DONE,
			purchase.getOrderName(),
			info.getApprovedAt().toLocalDateTime()
		);

		purchaseRepository.save(purchase);
		log.info("결제 승인 처리 완료: {}", purchase.getId());
	}

	private void handleCanceled(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.DONE) {
			List<Ticket> tickets = ticketRepository.findAllByPurchaseId(purchase.getId());
			tickets.forEach(ticket -> {
				Seat seat = seatRepository.findByTicketId(ticket.getId()).orElse(null);
				if (seat != null) {
					seat.setAvailable(true);
					seat.setTicket(null);
					seatRepository.save(seat);
				}
			});
			ticketRepository.deleteAll(tickets);
			purchase.setPaymentStatus(PaymentStatusEnum.CANCELED);
			purchaseRepository.save(purchase);
			log.info("결제 취소 처리 완료: {}", purchase.getId());
		}
	}

	private void handleExpired(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.IN_PROGRESS) {
			purchase.setPaymentStatus(PaymentStatusEnum.EXPIRED);
			purchaseRepository.save(purchase);
			log.info("결제 만료 처리 완료: {}", purchase.getId());
		}
	}
}