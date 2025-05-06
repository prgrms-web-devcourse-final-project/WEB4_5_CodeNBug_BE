package org.codeNbug.mainserver.external.toss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CanceledPaymentInfo {
	private String paymentKey;
	private String orderId;
	private String status;
	private Integer cancelAmount;
	private String canceledAt;
	private String cancelReason;
	private Integer refundableAmount;
	private Receipt receipt;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Receipt {
		private String url;
	}
}