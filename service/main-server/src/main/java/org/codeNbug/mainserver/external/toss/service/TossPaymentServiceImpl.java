package org.codeNbug.mainserver.external.toss.service;

import java.util.Base64;
import java.util.Map;

import org.codeNbug.mainserver.external.toss.dto.CanceledPaymentInfo;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Toss 결제 관련 유틸리티 로직을 담당하는 구현 클래스
 */
@Component
public class TossPaymentServiceImpl implements TossPaymentService {

	private final RestTemplate restTemplate;
	private final String secretKey;
	private final String tossApiUrl;

	public TossPaymentServiceImpl(RestTemplate restTemplate,
		@Value("${payment.toss.secret-key}") String secretKey,
		@Value("${payment.toss.api-url}") String tossApiUrl) {
		this.restTemplate = restTemplate;
		this.secretKey = secretKey;
		this.tossApiUrl = tossApiUrl;
	}

	private HttpHeaders createAuthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
		headers.set("Authorization", "Basic " + encoded);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	/**
	 * Toss 서버에 결제 승인을 요청하고 결과 정보를 반환
	 */
	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount) {
		String url = tossApiUrl + "/confirm";
		Map<String, Object> body = Map.of(
			"paymentKey", paymentKey,
			"orderId", orderId,
			"amount", amount
		);
		return postToToss(url, body, ConfirmedPaymentInfo.class);
	}

	/**
	 * Toss 서버에 전액 결제 취소를 요청하고 결과 정보를 반환
	 */
	@Override
	public CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason) {
		String url = tossApiUrl + "/" + paymentKey + "/cancel";
		Map<String, Object> body = Map.of("cancelReason", cancelReason);
		return postToToss(url, body, CanceledPaymentInfo.class);
	}

	/**
	 * 공통 Toss 서버 요청 로직
	 */
	private <T> T postToToss(String url, Map<String, Object> body, Class<T> clazz) {
		try {
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createAuthHeaders());
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.findAndRegisterModules();
			return objectMapper.readValue(response.getBody(), clazz);
		} catch (Exception e) {
			throw new RuntimeException("Toss 응답 파싱 실패: " + e.getMessage(), e);
		}
	}
}