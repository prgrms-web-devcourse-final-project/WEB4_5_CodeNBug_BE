package org.codeNbug.mainserver.domain.purchase.controller;

import java.io.IOException;

import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.ConfirmPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PurchaseController {
	private final PurchaseService purchaseService;

	/**
	 * 티켓 구매 전 사전 등록
	 *
	 * @param request 이벤트 id
	 * @return 결제 준비 완료 응답
	 */
	@PostMapping("/init")
	public ResponseEntity<RsData<InitiatePaymentResponse>> initiatePayment(
		@RequestBody InitiatePaymentRequest request
	) {
		Long userId = SecurityUtil.getCurrentUserId();
		InitiatePaymentResponse response = purchaseService.initiatePayment(request, userId);
		return ResponseEntity.ok(new RsData<>("200", "결제 준비 완료", response));
	}

	@PostMapping("/confirm")
	public ResponseEntity<RsData<ConfirmPaymentResponse>> confirmPayment(
		@RequestBody ConfirmPaymentRequest request
	) throws IOException, InterruptedException {
		ConfirmPaymentResponse response = purchaseService.confirmPayment(
			request.getPaymentKey(),
			request.getOrderId(),
			request.getAmount()
		);
		return ResponseEntity.ok(new RsData<>("200", "결제 승인 완료", response));
	}

	/**
	 * 티켓 구매 이후 결제 정보 조회
	 *
	 * @param paymentKey 결제의 paymentKey
	 * @return 결제 준비 완료 응답
	 */
	// @GetMapping("/status")
	// public ResponseEntity<RsData<TicketPurchaseResponse>> getPaymentInfo(
	// 	@RequestParam String paymentKey
	// ) {
	// 	TicketPurchaseResponse response = purchaseService.getPaymentInfo(paymentKey);
	// 	return ResponseEntity.ok(new RsData<>("200", "결제 정보 반환 완료", response));
	// }
}
