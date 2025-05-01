package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
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
	public ConfirmedPaymentInfo confirmPayment(String paymentUuid, String orderId, String orderName, Integer amount,
		String status)
		throws InterruptedException, IOException {
		HttpResponse<String> tossResponse = tossPaymentClient.requestConfirm(paymentUuid, orderId, orderName, amount,
			status);
		if (tossResponse.statusCode() != 200) {
			throw new IllegalStateException("Toss 결제 승인 실패: " + tossResponse.body());
		}
		return objectMapper.readValue(tossResponse.body(), ConfirmedPaymentInfo.class);
	}

	/**
	 * 이벤트 조회
	 */
	@Override
	public Event getEvent(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("이벤트가 존재하지 않습니다."));
	}

	/**
	 * 유저 조회
	 */
	@Override
	public User getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
	}
}