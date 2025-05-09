package org.codeNbug.mainserver.domain.purchase.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelPaymentResponse {
	private String paymentKey;
	private String orderId;
	private String status;
	private String method;
	private Integer totalAmount;
	private String receiptUrl;
	private List<CancelDetail> cancels;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CancelDetail {
		private Integer cancelAmount;
		private LocalDateTime canceledAt;
		private String cancelReason;
	}
}