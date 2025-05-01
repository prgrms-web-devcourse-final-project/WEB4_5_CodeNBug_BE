package org.codeNbug.mainserver.domain.purchase.service;

import java.util.UUID;

import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentRequest;
import org.codeNbug.mainserver.domain.purchase.dto.InitiatePaymentResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {
	private final PurchaseRepository purchaseRepository;
	private final UserRepository userRepository;
	private final EventRepository eventRepository;

	/**
	 * 결제 사전 등록 처리
	 * - 결제 UUID를 생성하고 결제 상태를 '진행 중'으로 설정하여 저장
	 *
	 * @param request 이벤트 ID 정보가 포함된 요청 DTO
	 * @param userId 현재 로그인한 사용자 ID
	 * @return 결제 UUID 및 상태 정보를 포함한 응답 DTO
	 */
	public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("사용자가 존재하지 않습니다."));
		eventRepository.findById(userId)
			.orElseThrow(() -> new IllegalStateException("행사가 존재하지 않습니다."));

		String uuid = UUID.randomUUID().toString();

		Purchase purchase = Purchase.builder()
			.paymentUuid(uuid)
			.paymentStatus(PaymentStatusEnum.IN_PROGRESS)
			.amount(request.getAmount())
			.user(user)
			.eventId(request.getEventId())
			.ticketCount(request.getTicketCount()) // 미지정석인 경우
			.selectedSeatIds(request.getSeatIds())
			.build();

		purchaseRepository.save(purchase);

		return new InitiatePaymentResponse(purchase.getId(), purchase.getPaymentStatus().name());
	}
}