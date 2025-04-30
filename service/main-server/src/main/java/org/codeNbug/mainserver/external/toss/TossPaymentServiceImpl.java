package org.codeNbug.mainserver.external.toss;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public abstract class TossPaymentServiceImpl implements TossPaymentService {
	private final ObjectMapper objectMapper;
	private final TossPaymentClient tossPaymentClient;

	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentUuid, String orderId, String orderName, String amount) {
		try {
			HttpResponse<String> response = tossPaymentClient.requestConfirm(paymentUuid, orderId, orderName, amount);
			if (response.statusCode() != 200) {
				throw new IllegalStateException("Toss 승인 실패: " + response.body());
			}
			return objectMapper.readValue(response.body(), ConfirmedPaymentInfo.class);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Toss 결제 승인 중 오류 발생", e);
		}
	}
}
