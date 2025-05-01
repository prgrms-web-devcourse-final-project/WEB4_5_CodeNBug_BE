package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

	private final ObjectMapper objectMapper;

	@Value("${payment.toss.secret-key}")
	private String TOSS_SECRET_KEY;

	@Value("${payment.toss.api-url}")
	private String TOSS_API_URL;

	/**
	 * Toss 결제 승인 요청
	 *
	 * @param paymentUuid Toss에서 받은 결제 키
	 * @param orderId     사용자 주문 ID
	 * @param amount      결제 금액
	 * @param status
	 * @return HttpResponse 결제 승인 응답
	 * @throws IOException,InterruptedException
	 */
	public HttpResponse<String> requestConfirm(String paymentUuid, String orderId, String orderName, Integer amount,
		String status)
		throws IOException, InterruptedException {

		String body = objectMapper.writeValueAsString(Map.of(
			"paymentKey", paymentUuid,
			"orderId", orderId,
			"orderName", orderName,
			"amount", amount
		));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(TOSS_API_URL))
			.header("Authorization", getAuthorization())
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * Toss API 요청에 필요한 인증 헤더 생성
	 *
	 * @return 인증 헤더
	 */
	private String getAuthorization() {
		String base64 = Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + base64;
	}
}