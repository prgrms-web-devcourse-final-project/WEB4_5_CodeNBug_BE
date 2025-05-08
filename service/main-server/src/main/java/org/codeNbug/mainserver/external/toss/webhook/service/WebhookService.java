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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

	private final ObjectMapper objectMapper;
	private final PurchaseRepository purchaseRepository;

	@Value("${payment.toss.secret-key}")
	private String tossSecretKey;

	public void handleWebhook(String payload, String signature) {
		try {
			if (!isValidSignature(payload, signature)) {
				throw new SecurityException("Toss 시그니처 검증 실패");
			}

			JsonNode root = objectMapper.readTree(payload);
			String status = root.path("data").path("status").asText();

			switch (status) {
				case "DONE":
					handleApproved(root);
					break;
				case "CANCELED":
					handleCanceled(root);
					break;
				case "EXPIRED":
					handleExpired(root);
					break;
				default:
					log.warn("처리되지 않은 결제 상태: {}", status);
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

	public void handleApproved(JsonNode root) {
		JsonNode data = root.path("data");
		String paymentKey = data.path("paymentKey").asText();

		Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("[DONE] paymentKey 로 구매 정보를 찾을 수 없습니다."));

		purchase.setPaymentStatus(PaymentStatusEnum.DONE);
		purchaseRepository.save(purchase);
		log.info("[Webhook] 결제 승인 처리 완료: paymentKey = {}", paymentKey);
	}

	private void handleCanceled(JsonNode root) {
		JsonNode data = root.path("data");
		String paymentKey = data.path("paymentKey").asText();

		Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("[CANCELED] paymentKey 로 구매 정보를 찾을 수 없습니다."));

		purchase.setPaymentStatus(PaymentStatusEnum.CANCELED);
		purchaseRepository.save(purchase);

		log.info("결제 취소 처리 완료: paymentKey = {}", paymentKey);
	}

	private void handleExpired(JsonNode root) {
		JsonNode data = root.path("data");
		String paymentKey = data.path("paymentKey").asText();

		Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
			.orElseThrow(() -> new IllegalArgumentException("[EXPIRED] paymentKey 로 구매 정보를 찾을 수 없습니다."));

		purchase.setPaymentStatus(PaymentStatusEnum.EXPIRED);
		purchaseRepository.save(purchase);
		log.info("결제 만료 처리 완료: {}", purchase.getId());
	}
}