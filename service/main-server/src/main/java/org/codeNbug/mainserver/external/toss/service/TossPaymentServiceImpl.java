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

import lombok.RequiredArgsConstructor;

/**
 * Toss 결제 관련 유틸리티 로직을 담당하는 구현 클래스
 */
@Component
@RequiredArgsConstructor
public class TossPaymentServiceImpl implements TossPaymentService {

	private final RestTemplate restTemplate;

	@Value("${payment.toss.secret-key}")
	private String TOSS_SECRET_KEY;

	@Value("${payment.toss.api-url}")
	private String TOSS_API_URL;

	/**
	 * Toss 서버에 결제 승인을 요청하고 결과 정보를 반환
	 */
	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount) {
		String url = TOSS_API_URL + "/confirm";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes()));
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = Map.of(
			"paymentKey", paymentKey,
			"orderId", orderId,
			"amount", amount
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(
			url, HttpMethod.POST, request, String.class
		);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.findAndRegisterModules();

			return objectMapper.readValue(response.getBody(), ConfirmedPaymentInfo.class);
		} catch (Exception e) {
			throw new RuntimeException("Toss 응답 파싱 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * Toss 서버에 전액 결제 취소를 요청하고 결과 정보를 반환
	 */
	@Override
	public CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason) {
		String url = TOSS_API_URL + "/" + paymentKey + "/cancel";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes()));
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = Map.of(
			"cancelReason", cancelReason
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(
			url, HttpMethod.POST, request, String.class
		);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.findAndRegisterModules();
			return objectMapper.readValue(response.getBody(), CanceledPaymentInfo.class);
		} catch (Exception e) {
			throw new RuntimeException("Toss 전액 취소 응답 파싱 실패: " + e.getMessage(), e);
		}
	}

	/**
	 * Toss 서버에 부분 결제 취소를 요청하고 결과 정보를 반환
	 */
	@Override
	public CanceledPaymentInfo cancelPartialPayment(String paymentKey, String cancelReason, Integer cancelAmount) {
		String url = TOSS_API_URL + "/" + paymentKey + "/cancel";

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes()));
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = Map.of(
			"cancelReason", cancelReason,
			"cancelAmount", cancelAmount
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.exchange(
			url, HttpMethod.POST, request, String.class
		);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.findAndRegisterModules();
			return objectMapper.readValue(response.getBody(), CanceledPaymentInfo.class);
		} catch (Exception e) {
			throw new RuntimeException("Toss 부분 취소 응답 파싱 실패: " + e.getMessage(), e);
		}
	}
}