package org.codeNbug.mainserver.external.toss.webhook.service;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Webhook에서 받는 데이터 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {
	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;

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

	private void handlePaymentApproved(Purchase purchase) {
		if (purchase.getPaymentStatus() != PaymentStatusEnum.IN_PROGRESS) {
			log.info("결제 승인 처리: IN_PROGRESS -> DONE");
			purchase.setPaymentStatus(PaymentStatusEnum.DONE);
		}
	}

	private void handlePaymentExpired(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.IN_PROGRESS) {
			log.warn("결제 만료 처리: IN_PROGRESS -> EXPIRED");
			purchase.setPaymentStatus(PaymentStatusEnum.EXPIRED);
		}
	}

	private void handlePaymentCanceled(Purchase purchase) {
		if (purchase.getPaymentStatus() == PaymentStatusEnum.DONE) {
			log.info("결제 취소 처리: DONE -> CANCELED");
			purchase.setPaymentStatus(PaymentStatusEnum.CANCELLED);
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
