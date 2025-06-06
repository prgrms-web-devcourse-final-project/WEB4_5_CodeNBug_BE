package org.codeNbug.mainserver.external.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Toss 결제 승인 응답을 담는 DTO (TOSS API 응답)
 * Toss API 문서에 명시된 필드를 기반으로 매핑
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmedPaymentInfo {
	private String paymentKey;
	private String orderId;
	private String orderName;
	private Integer totalAmount;
	private String status;
	private String method;
	private String approvedAt;
	private Receipt receipt;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Receipt {
		private String url;
	}
}
