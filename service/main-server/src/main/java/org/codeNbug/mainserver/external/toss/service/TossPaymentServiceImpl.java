package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;
import java.net.http.HttpResponse;

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
}