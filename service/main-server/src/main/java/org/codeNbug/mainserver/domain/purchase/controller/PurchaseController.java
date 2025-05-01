package org.codeNbug.mainserver.domain.purchase.controller;

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
@RequestMapping("/api/v1/purchase")
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
}
