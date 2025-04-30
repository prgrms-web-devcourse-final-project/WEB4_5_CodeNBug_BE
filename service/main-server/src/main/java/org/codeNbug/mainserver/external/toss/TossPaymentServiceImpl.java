package org.codeNbug.mainserver.external.toss;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Toss 결제 관련 유틸리티 로직을 담당하는 구현 클래스
 */
@Component
@RequiredArgsConstructor
public class TossPaymentServiceImpl implements TossPaymentService {

	private final ObjectMapper objectMapper;
	private final TossPaymentClient tossPaymentClient;
	private final PurchaseRepository purchaseRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	/**
	 * Toss 서버에 결제 승인을 요청하고 결과 정보를 반환
	 */
	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentUuid, String orderId, String orderName, Integer amount)
		throws InterruptedException, IOException {
		HttpResponse<String> tossResponse = tossPaymentClient.requestConfirm(paymentUuid, orderId, orderName, amount);
		if (tossResponse.statusCode() != 200) {
			throw new IllegalStateException("Toss 결제 승인 실패: " + tossResponse.body());
		}
		return objectMapper.readValue(tossResponse.body(), ConfirmedPaymentInfo.class);
	}

	/**
	 * Toss 결제 응답 기반으로 결제 정보를 업데이트
	 */
	public Purchase loadAndUpdatePurchase(ConfirmedPaymentInfo info, String paymentMethod, String orderName) {
		Purchase purchase = purchaseRepository.findByPaymentUuid(info.getPaymentUuid())
			.orElseThrow(() -> new IllegalStateException("사전 등록된 결제가 없습니다."));

		purchase.updatePaymentInfo(
			Integer.parseInt(info.getTotalAmount()),
			PaymentMethodEnum.valueOf(paymentMethod),
			PaymentStatusEnum.valueOf(info.getStatus()),
			orderName,
			LocalDateTime.parse(info.getApprovedAt())
		);
		return purchase;
	}

	/**
	 * 유저 조회
	 */
	public Event getEvent(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("이벤트가 존재하지 않습니다."));
	}

	/**
	 * 이벤트 조회
	 */
	public User getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
	}
}