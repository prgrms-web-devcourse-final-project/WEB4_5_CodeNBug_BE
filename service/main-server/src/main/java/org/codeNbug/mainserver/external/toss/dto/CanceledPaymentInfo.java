package org.codeNbug.mainserver.external.toss.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CanceledPaymentInfo {
	private String paymentKey;
	private String orderId;
	private String status;
	private String method;
	private Integer totalAmount;
	private Receipt receipt;
	private List<CancelDetail> cancels;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Receipt {
		private String url;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CancelDetail {
		private Integer cancelAmount;
		private String canceledAt;
		private String cancelReason;
	}
}