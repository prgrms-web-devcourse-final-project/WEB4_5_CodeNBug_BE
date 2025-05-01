package org.codeNbug.mainserver.domain.purchase.controller;

import java.io.IOException;
import java.util.Map;

import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.NonSelectTicketPurchaseResponse;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseRequest;
import org.codeNbug.mainserver.domain.purchase.dto.SelectTicketPurchaseResponse;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/purchase")
public class PurchaseController {

	private final ObjectMapper objectMapper;
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

	/**
	 * 티켓 구매 (결제 완료 후 Toss 승인 요청)
	 *
	 * @param type "non-select" or "select" 분기 처리
	 * @param requestMap 결제 관련 정보 (Toss 결제 후 전달됨)
	 * @return 티켓 구매 응답
	 */
	@PostMapping("/tickets")
	public ResponseEntity<RsData<Object>> purchaseTicket(
		@RequestParam("type") String type,
		@RequestBody Map<String, Object> requestMap
	) throws IOException, InterruptedException {
		Long userId = SecurityUtil.getCurrentUserId();
		if (type.equals("non-select")) {
			NonSelectTicketPurchaseRequest request = objectMapper.convertValue(requestMap,
				NonSelectTicketPurchaseRequest.class);
			NonSelectTicketPurchaseResponse response = purchaseService.purchaseNonSelectTicket(request, userId);
			return ResponseEntity.ok(new RsData<>("200", "구매 완료", response));
		} else if (type.equals("select")) {
			SelectTicketPurchaseRequest request = objectMapper.convertValue(requestMap,
				SelectTicketPurchaseRequest.class);
			SelectTicketPurchaseResponse response = purchaseService.purchaseSelectedSeats(request, userId);
			return ResponseEntity.ok(new RsData<>("200", "구매 완료", response));
		} else {
			return ResponseEntity.badRequest().body(new RsData<>("400", "지원하지 않는 티켓 타입입니다."));
		}
	}
}
