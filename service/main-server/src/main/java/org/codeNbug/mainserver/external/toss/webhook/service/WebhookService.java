package org.codeNbug.mainserver.external.toss.webhook.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.external.toss.dto.CanceledPaymentInfo;
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
	private final PurchaseService purchaseService;
	private final TossPaymentService tossPaymentService;

	@Value("${payment.toss.secret-key}")
	private String tossSecretKey;

	public void handleWebhook(String payload, String signature) throws Exception {
		try {
			if (!isValidSignature(payload, signature)) {
				throw new SecurityException("Toss 시그니처 검증 실패");
			}

			JsonNode root = objectMapper.readTree(payload);
			String status = root.path("data").path("status").asText();
			String paymentKey = root.path("data").path("paymentKey").asText();

			Purchase purchase = purchaseRepository.findByPaymentUuid(paymentKey)
				.orElseThrow(() -> new IllegalStateException("해당 결제를 찾을 수 없습니다."));

			switch (status) {
				case "DONE" -> handleApproved(purchase, root);
				case "CANCELED" -> handleCanceled(purchase, root);
				case "EXPIRED" -> handleExpired(purchase, root);
				default -> log.warn("처리되지 않은 결제 상태: {}", status);
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

	private void handleApproved(Purchase purchase, JsonNode root) throws IOException, InterruptedException {
		log.info("[handleApproved] paymentUuid: {}, orderId: {}, amount: {}", purchase.getPaymentUuid(),
			purchase.getOrderId(), purchase.getAmount());

		JsonNode data = root.path("data");
		ConfirmedPaymentInfo info = tossPaymentService.confirmPayment(
			data.path("paymentKey").asText(),
			data.path("orderId").asText(),
			data.path("orderName").asText(),
			data.path("method").asText(),
			data.path("totalAmount").asInt(),
			LocalDateTime.parse(data.path("approvedAt").asText()),
			data.path("receipt").path("url").asText()
		);

		purchaseService.confirmPayment(info, purchase.getId(), purchase.getUser().getUserId());

		log.info("결제 승인 처리 완료: {}", purchase.getId());
	}

	private void handleCanceled(Purchase purchase, JsonNode root) {
		log.info("handleCanceled: {}", purchase);

		JsonNode cancelsNode = root.path("data").path("cancels");
		if (!cancelsNode.isArray()) {
			log.warn("취소 내역이 없습니다.");
			return;
		}

		List<CanceledPaymentInfo.CancelDetail> cancelDetails = new ArrayList<>();
		for (JsonNode cancelNode : cancelsNode) {
			cancelDetails.add(new CanceledPaymentInfo.CancelDetail(
				cancelNode.path("cancelAmount").asInt(),
				cancelNode.path("cancelReason").asText(),
				cancelNode.path("canceledAt").asText()
			));
		}

		JsonNode data = root.path("data");

		String receiptUrl = data.path("receipt").path("url").asText();
		CanceledPaymentInfo.Receipt receipt = receiptUrl != null ? new CanceledPaymentInfo.Receipt(receiptUrl) : null;

		CanceledPaymentInfo info = new CanceledPaymentInfo(
			data.path("paymentKey").asText(),
			data.path("orderId").asText(),
			data.path("status").asText(),
			data.path("method").asText(),
			data.path("totalAmount").asInt(),
			data.path("balanceAmount").asInt(),
			data.path("isPartialCancelable").isBoolean(),
			receipt,
			cancelDetails
		);

		purchaseService.cancelPayment(new CancelPaymentRequest(
			data.path("cancelReason").asText(),
			data.path("cancelAmount").asInt()
		), purchase.getPaymentUuid(), purchase.getUser().getUserId());

		log.info("결제 취소 처리 완료: {}", purchase.getId());
	}

	private void handleExpired(Purchase purchase, JsonNode root) {
		log.info("handleExpired: {}", purchase);

		purchase.setPaymentStatus(PaymentStatusEnum.EXPIRED);
		purchaseRepository.save(purchase);
		log.info("결제 만료 처리 완료: {}", purchase.getId());
	}
}