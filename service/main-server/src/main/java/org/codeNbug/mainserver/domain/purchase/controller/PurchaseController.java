package org.codeNbug.mainserver.domain.purchase.controller;

import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.CancelPaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	// /**
	//  * 티켓 구매 승인
	//  *
	//  * @param request 결제 준비 완료된 데이터
	//  * @return 결제 승인 완료 응답
	//  */
	// @PostMapping("/confirm")
	// public ResponseEntity<RsData<ConfirmPaymentResponse>> confirmPayment(
	// 	@RequestBody ConfirmPaymentRequest request
	// ) throws IOException, InterruptedException {
	// 	Long userId = SecurityUtil.getCurrentUserId();
	// 	ConfirmPaymentResponse response = purchaseService.confirmPayment(request, userId);
	// 	return ResponseEntity.ok(new RsData<>("200", "결제 승인 완료", response));
	// }

	/**
	 * 유저 측 티켓 결제 취소
	 *
	 * @param paymentKey 결제 키 (Toss에서 발급된 고유 키)
	 * @param request 취소 사유 및 취소 금액
	 * @return 결제 취소 완료 응답
	 */
	@PostMapping("/{paymentKey}/cancel")
	public ResponseEntity<RsData<CancelPaymentResponse>> cancelPayment(
		@PathVariable String paymentKey,
		@RequestBody CancelPaymentRequest request
	) {
		Long userId = SecurityUtil.getCurrentUserId();
		CancelPaymentResponse response = purchaseService.cancelPayment(request, paymentKey, userId);
		return ResponseEntity.ok(new RsData<>("200", "결제 취소 완료", response));
	}
}
