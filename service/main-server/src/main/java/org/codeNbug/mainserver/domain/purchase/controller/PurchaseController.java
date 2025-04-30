package org.codeNbug.mainserver.domain.purchase.controller;

import java.io.IOException;

import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseResponse;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
public class PurchaseController {

	private final PurchaseService purchaseService;

	/**
	 * 티켓 구매 전 사전 등록
	 *
	 * @param request 이벤트 id
	 * @return 결제 준비 완료 응답
	 */
	@PostMapping("/init")
	public ResponseEntity<RsData> initiatePayment(
		@RequestBody InitiatePaymentRequest request
	) {
		Long userId = SecurityUtil.getCurrentUserId();
		InitiatePaymentResponse response = purchaseService.initiatePayment(request, userId);
		return ResponseEntity.ok(new RsData("200", "결제 준비 완료", response));
	}

	/**
	 * 미지정석 티켓 구매 (결제 완료 후 Toss 승인 요청)
	 *
	 * @param type "non-select"이어야 함
	 * @param request 결제 관련 정보 (Toss 결제 후 전달됨)
	 * @return 티켓 구매 응답
	 */
	@PostMapping
	public ResponseEntity<RsData> purchaseTicket(
		@RequestParam("type") String type,
		@RequestBody NonSelectTicketPurchaseRequest request
	) throws IOException, InterruptedException {
		if (!type.equals("non-select")) {
			return ResponseEntity.badRequest()
				.body(new RsData<>("400", "지원하지 않는 티켓 타입입니다."));
		}

		Long userId = SecurityUtil.getCurrentUserId();
		NonSelectTicketPurchaseResponse response = purchaseService.purchaseNonSelectTicket(request, userId);
		return ResponseEntity.ok(new RsData<>("200", "구매 완료", response));
	}
}
