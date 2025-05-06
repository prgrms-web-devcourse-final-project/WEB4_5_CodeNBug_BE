package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelPaymentResponse {
	private String paymentKey;
	private String orderId;
	private String status;
	private Integer cancelAmount;
	private String canceledAt;
	private String cancelReason;
	private String receiptUrl;
}
