package org.codeNbug.mainserver.domain.manager.controller;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.service.EventEditService;
import org.codeNbug.mainserver.domain.manager.dto.*;
import org.codeNbug.mainserver.domain.manager.service.EventDeleteService;
import org.codeNbug.mainserver.domain.manager.service.EventRegisterService;
import org.codeNbug.mainserver.domain.manager.service.ManagerEventSearchService;
import org.codeNbug.mainserver.domain.manager.service.ManagerPurchasesService;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager")
public class ManagerController {
	private final EventRegisterService eventRegisterService;
	private final EventEditService eventEditService;
	private final EventDeleteService eventDeleteService;
	private final ManagerEventSearchService eventSearchService;
	private final ManagerPurchasesService managerPurchasesService;
	private final PurchaseService purchaseService;

	/**
	 * 이벤트 등록 API
	 * @param request 이벤트 등록 요청 DTO
	 * @return 성공 시 RsData<EventRegisterResponse> 포맷으로 응답
	 */
	@RoleRequired({UserRole.MANAGER, UserRole.ADMIN})
	@PostMapping
	public ResponseEntity<RsData<EventRegisterResponse>> eventRegister(@RequestBody EventRegisterRequest request) {
		EventRegisterResponse response = eventRegisterService.registerEvent(request, SecurityUtil.getCurrentUserId());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 등록 성공",
			response
		));
	}

	@RoleRequired({UserRole.MANAGER, UserRole.ADMIN})
	@PutMapping("/events/{eventId}")
	public ResponseEntity<RsData<EventRegisterResponse>> updateEvent(
		@PathVariable Long eventId,
		@RequestBody EventRegisterRequest request
	) {

		EventRegisterResponse response = eventEditService.editEvent(eventId, request, SecurityUtil.getCurrentUserId());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 수정 성공",
			response
		));
	}

	@RoleRequired({UserRole.MANAGER, UserRole.ADMIN})
	@PatchMapping("/events/{eventId}")
	public ResponseEntity<RsData<Void>> deleteEvent(@PathVariable Long eventId) {
		eventDeleteService.deleteEvent(eventId, SecurityUtil.getCurrentUserId());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"이벤트 삭제 성공",
			null
		));
	}

	@RoleRequired({UserRole.MANAGER})
	@GetMapping("/events/me")
	public ResponseEntity<RsData<List<ManagerEventListResponse>>> searchManagerEventList() {
		List<ManagerEventListResponse> response = eventSearchService.searchEventList(SecurityUtil.getCurrentUser());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"매니저 이벤트 목록 조회 성공",
			response
		));
	}

	@RoleRequired({UserRole.MANAGER})
	@GetMapping("/events/{eventId}/purchases")
	public ResponseEntity<RsData<List<EventPurchaseResponse>>> eventPurchaseList(
		@PathVariable Long eventId
	) {
		List<EventPurchaseResponse> response = managerPurchasesService.getEventPurchaseList(eventId,
			SecurityUtil.getCurrentUser());
		return ResponseEntity.ok(new RsData<>(
			"200",
			"구매 내역 조회 성공",
			response
		));
	}

	@RoleRequired({UserRole.MANAGER})
	@PostMapping("/purchases/{eventId}/refund")
	public ResponseEntity<RsData<List<ManagerRefundResponse>>> managerRefund(
			@PathVariable Long eventId,
			@RequestBody ManagerRefundRequest request
	) {
		List<ManagerRefundResponse> responses = purchaseService.managerCancelPayment(request, eventId, SecurityUtil.getCurrentUser());
		return ResponseEntity.ok(new RsData<>(
				"200",
				"매니저 환불 성공",
				responses
		));
	}

}
